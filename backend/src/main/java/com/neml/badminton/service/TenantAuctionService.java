package com.neml.badminton.service;

import com.neml.badminton.dto.Dtos.*;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.websocket.AuctionBroadcaster;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class TenantAuctionService {
    private final AuctionRepository auctions; private final AuctionStateRepository states;
    private final PlayerRepository players; private final TeamRepository teams; private final BidRepository bids;
    private final TournamentSettingsRepository settings; private final AuctionBroadcaster broadcaster;

    public TenantAuctionService(AuctionRepository auctions, AuctionStateRepository states, PlayerRepository players,
            TeamRepository teams, BidRepository bids, TournamentSettingsRepository settings, AuctionBroadcaster broadcaster) {
        this.auctions=auctions; this.states=states; this.players=players; this.teams=teams; this.bids=bids;
        this.settings=settings; this.broadcaster=broadcaster;
    }

    public AuctionStateDto state(UUID championshipId, UUID auctionId) {
        Auction auction = auction(championshipId, auctionId); AuctionState s = state(auction);
        List<BidDto> history = bids.findTop50ByAuctionIdOrderByCreatedAtDesc(auctionId).stream().map(BidDto::from).toList();
        BidDto highest = s.getCurrentPlayer() == null ? null : bids
                .findFirstByAuctionIdAndPlayerAndActiveTrueOrderByCreatedAtDesc(auctionId, s.getCurrentPlayer())
                .map(BidDto::from).orElse(null);
        List<TeamDto> teamDtos = teams.findAllByChampionshipIdOrderByName(championshipId).stream().map(TeamDto::from).toList();
        int remaining = players.findAllByAuctionIdAndStatusOrderByAuctionOrderAsc(auctionId, PlayerStatus.AVAILABLE).size();
        return new AuctionStateDto(s.getStatus(), s.getCurrentPlayer() == null ? null : PlayerDto.from(s.getCurrentPlayer()),
                highest, history, teamDtos, remaining, s.getBidDeadline(), s.getTimerSeconds());
    }

    @Transactional public AuctionStateDto start(UUID cid, UUID aid) {
        Auction a=auction(cid,aid); AuctionState s=state(a); s.setStatus(AuctionStatus.RUNNING); a.setStatus(AuctionStatus.RUNNING);
        if (s.getCurrentPlayer()==null) selectNext(a,s); deadline(s); save(a,s); return broadcast(cid,aid,state(cid,aid));
    }
    @Transactional public AuctionStateDto pause(UUID cid, UUID aid) { return status(cid,aid,AuctionStatus.PAUSED); }
    @Transactional public AuctionStateDto resume(UUID cid, UUID aid) {
        Auction a=auction(cid,aid); AuctionState s=state(a); s.setStatus(AuctionStatus.RUNNING); a.setStatus(AuctionStatus.RUNNING);
        deadline(s); save(a,s); return broadcast(cid,aid,state(cid,aid));
    }
    @Transactional public AuctionStateDto status(UUID cid, UUID aid, AuctionStatus status) {
        Auction a=auction(cid,aid); AuctionState s=state(a); s.setStatus(status); a.setStatus(status);
        if(status!=AuctionStatus.RUNNING)s.setBidDeadline(null); save(a,s); return broadcast(cid,aid,state(cid,aid));
    }

    @Transactional public AuctionStateDto bid(UUID cid, UUID aid, PlaceBidRequest req) {
        Auction a=auction(cid,aid); AuctionState s=state(a);
        if(s.getStatus()!=AuctionStatus.RUNNING) bad("Auction is not running");
        Player p=players.findByIdAndChampionshipId(req.playerId(),cid).orElseThrow(()->notFound("Player"));
        Team t=teams.findByIdAndChampionshipId(req.teamId(),cid).orElseThrow(()->notFound("Team"));
        if(p.getAuction()==null || !p.getAuction().getId().equals(aid) || s.getCurrentPlayer()==null || !p.getId().equals(s.getCurrentPlayer().getId())) bad("Player is not on this auction block");
        BigDecimal increment=new BigDecimal("500000");
        BigDecimal previous=bids.findFirstByAuctionIdAndPlayerAndActiveTrueOrderByCreatedAtDesc(aid,p).map(Bid::getAmount).orElse(p.getBasePrice().subtract(increment));
        if(req.amount()==null || req.amount().compareTo(previous.add(increment))<0) bad("Bid is below the minimum increment");
        TournamentSettings cfg=settings.findByChampionshipId(cid).orElseThrow(()->notFound("Tournament settings"));
        int filled=(t.getMaleCount()==null?0:t.getMaleCount())+(t.getFemaleCount()==null?0:t.getFemaleCount());
        if(filled>=cfg.getMaxSquadSize()) bad("Team squad is full");
        int slots=cfg.getMaxSquadSize()-filled-1;
        BigDecimal reserve=p.getBasePrice().multiply(BigDecimal.valueOf(slots));
        if(t.getPurseRemaining().subtract(req.amount()).compareTo(reserve)<0) bad("Insufficient purse reserve");
        bids.save(Bid.builder().auction(a).player(p).team(t).amount(req.amount()).active(true).build());
        deadline(s); states.save(s);
        broadcaster.broadcastEvent(cid,aid,"BID_PLACED",Map.of("teamName",t.getName(),"amount",req.amount(),"player",p.getFullName()));
        return broadcast(cid,aid,state(cid,aid));
    }

    @Transactional public AuctionStateDto undo(UUID cid, UUID aid) {
        Auction a=auction(cid,aid); AuctionState s=state(a); if(s.getCurrentPlayer()==null) bad("No active player");
        Bid b=bids.findFirstByAuctionIdAndPlayerAndActiveTrueOrderByCreatedAtDesc(aid,s.getCurrentPlayer()).orElseThrow(()->notFound("Active bid"));
        b.setActive(false); bids.save(b); broadcaster.broadcastEvent(cid,aid,"BID_UNDONE",Map.of("amount",b.getAmount(),"teamName",b.getTeam().getName()));
        return broadcast(cid,aid,state(cid,aid));
    }

    @Transactional public AuctionStateDto sell(UUID cid, UUID aid) {
        Auction a=auction(cid,aid); AuctionState s=state(a); if(s.getCurrentPlayer()==null) bad("No active player");
        Player p=s.getCurrentPlayer(); Bid b=bids.findFirstByAuctionIdAndPlayerAndActiveTrueOrderByCreatedAtDesc(aid,p).orElseThrow(()->notFound("Active bid"));
        Team t=b.getTeam(); p.setTeam(t); p.setSoldPrice(b.getAmount()); p.setStatus(PlayerStatus.SOLD); players.save(p);
        t.setPurseRemaining(t.getPurseRemaining().subtract(b.getAmount()));
        if(p.getGender()==Gender.MALE)t.setMaleCount(t.getMaleCount()+1); else t.setFemaleCount(t.getFemaleCount()+1); teams.save(t);
        s.setCurrentPlayer(null); states.save(s); broadcaster.broadcastEvent(cid,aid,"PLAYER_SOLD",Map.of("player",p.getFullName(),"teamName",t.getName(),"amount",b.getAmount()));
        return next(cid,aid);
    }

    @Transactional public AuctionStateDto unsold(UUID cid, UUID aid) {
        Auction a=auction(cid,aid); AuctionState s=state(a); if(s.getCurrentPlayer()==null) bad("No active player");
        Player p=s.getCurrentPlayer(); p.setStatus(PlayerStatus.UNSOLD); players.save(p); s.setCurrentPlayer(null); states.save(s);
        broadcaster.broadcastEvent(cid,aid,"PLAYER_UNSOLD",Map.of("player",p.getFullName())); return next(cid,aid);
    }
    @Transactional public AuctionStateDto next(UUID cid, UUID aid) {
        Auction a=auction(cid,aid); AuctionState s=state(a); selectNext(a,s);
        if(s.getCurrentPlayer()==null){s.setStatus(AuctionStatus.COMPLETED);a.setStatus(AuctionStatus.COMPLETED);s.setBidDeadline(null);}
        else if(s.getStatus()==AuctionStatus.RUNNING)deadline(s); save(a,s); return broadcast(cid,aid,state(cid,aid));
    }

    @Transactional public AuctionStateDto setCurrent(UUID cid,UUID aid,UUID playerId){
        Auction a=auction(cid,aid);AuctionState s=state(a);Player p=players.findByIdAndChampionshipId(playerId,cid).orElseThrow(()->notFound("Player"));
        if(p.getAuction()==null||!p.getAuction().getId().equals(aid)||(p.getStatus()!=PlayerStatus.AVAILABLE&&p.getStatus()!=PlayerStatus.UNSOLD))bad("Player is not available in this auction");
        if(s.getCurrentPlayer()!=null&&s.getCurrentPlayer().getStatus()==PlayerStatus.ON_BLOCK){s.getCurrentPlayer().setStatus(PlayerStatus.AVAILABLE);players.save(s.getCurrentPlayer());}
        p.setStatus(PlayerStatus.ON_BLOCK);players.save(p);s.setCurrentPlayer(p);if(s.getStatus()==AuctionStatus.RUNNING)deadline(s);states.save(s);return broadcast(cid,aid,state(cid,aid));
    }
    @Transactional public AuctionStateDto resetUnsold(UUID cid,UUID aid){
        Auction a=auction(cid,aid);List<Player> list=players.findAllByAuctionIdAndStatusOrderByAuctionOrderAsc(aid,PlayerStatus.UNSOLD);list.forEach(p->p.setStatus(PlayerStatus.AVAILABLE));players.saveAll(list);
        broadcaster.broadcastEvent(cid,aid,"UNSOLD_RESET",Map.of("count",list.size()));return broadcast(cid,aid,state(cid,aid));
    }
    @Transactional public PlayerDto basePrice(UUID cid,UUID aid,UUID playerId,BigDecimal amount){
        auction(cid,aid);if(amount==null||amount.signum()<=0)bad("Base price must be positive");Player p=players.findByIdAndChampionshipId(playerId,cid).orElseThrow(()->notFound("Player"));
        if(p.getAuction()==null||!p.getAuction().getId().equals(aid)||p.getStatus()==PlayerStatus.SOLD)bad("Player cannot be edited in this auction");p.setBasePrice(amount);return PlayerDto.from(players.save(p));
    }
    public Map<String,Object> coinToss(UUID cid,UUID aid,List<UUID> teamIds){
        auction(cid,aid);if(teamIds==null||teamIds.size()<2)bad("At least two teams are required");UUID winner=teamIds.get(new Random().nextInt(teamIds.size()));Team team=teams.findByIdAndChampionshipId(winner,cid).orElseThrow(()->notFound("Team"));
        Map<String,Object> result=Map.of("winnerTeamId",winner.toString(),"winnerTeamName",team.getName());broadcaster.broadcastEvent(cid,aid,"COIN_TOSS",result);return result;
    }

    @Scheduled(fixedRate = 1000L)
    @Transactional
    public void timerSweep(){
        Instant now=Instant.now();
        for(AuctionState s:states.findAll()){
            if(s.getStatus()!=AuctionStatus.RUNNING||s.getCurrentPlayer()==null||s.getBidDeadline()==null||now.isBefore(s.getBidDeadline()))continue;
            Auction a=s.getAuction();UUID cid=a.getChampionship().getId();
            if(bids.findFirstByAuctionIdAndPlayerAndActiveTrueOrderByCreatedAtDesc(a.getId(),s.getCurrentPlayer()).isPresent())sell(cid,a.getId());else unsold(cid,a.getId());
        }
    }

    private void selectNext(Auction a,AuctionState s){
        List<Player> available=players.findAllByAuctionIdAndStatusOrderByAuctionOrderAsc(a.getId(),PlayerStatus.AVAILABLE);
        if(available.isEmpty()){s.setCurrentPlayer(null);return;} Player p=available.get(0);p.setStatus(PlayerStatus.ON_BLOCK);players.save(p);s.setCurrentPlayer(p);
    }
    private void deadline(AuctionState s){int seconds=s.getTimerSeconds()==null?30:s.getTimerSeconds();s.setTimerSeconds(seconds);s.setBidDeadline(Instant.now().plusSeconds(seconds));}
    private void save(Auction a,AuctionState s){auctions.save(a);states.save(s);}
    private Auction auction(UUID cid,UUID aid){return auctions.findByIdAndChampionshipId(aid,cid).orElseThrow(()->notFound("Auction"));}
    private AuctionState state(Auction a){return states.findByAuctionId(a.getId()).orElseGet(()->states.save(AuctionState.builder().auction(a).status(a.getStatus()).timerSeconds(30).build()));}
    private AuctionStateDto broadcast(UUID cid,UUID aid,AuctionStateDto dto){broadcaster.broadcastState(cid,aid,dto);return dto;}
    private void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);} private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m+" not found");}
}
