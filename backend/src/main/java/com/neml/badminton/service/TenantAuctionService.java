package com.neml.badminton.service;

import com.neml.badminton.dto.Dtos.*;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.websocket.AuctionBroadcaster;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class TenantAuctionService {
    private static final Logger log=LoggerFactory.getLogger(TenantAuctionService.class);
    private final AuctionRepository auctions; private final AuctionStateRepository states;
    private final PlayerRepository players; private final TeamRepository teams; private final BidRepository bids;
    private final TournamentSettingsRepository settings; private final AuctionBroadcaster broadcaster;
    private final TransactionTemplate transactions;
    private final SquadConfirmationService squadConfirmations;

    public TenantAuctionService(AuctionRepository auctions,AuctionStateRepository states,PlayerRepository players,
            TeamRepository teams,BidRepository bids,TournamentSettingsRepository settings,
            AuctionBroadcaster broadcaster,TransactionTemplate transactions,
            SquadConfirmationService squadConfirmations){
        this.auctions=auctions;this.states=states;this.players=players;this.teams=teams;this.bids=bids;
        this.settings=settings;this.broadcaster=broadcaster;this.transactions=transactions;
        this.squadConfirmations=squadConfirmations;
    }

    @Transactional public AuctionStateDto state(UUID cid,UUID aid){Auction a=auction(cid,aid);return snapshot(cid,a,auctionState(cid,aid));}
    @Transactional public List<BidDto> history(UUID cid,UUID aid){auction(cid,aid);return bids.findTop50ByAuctionIdAndChampionshipIdOrderBySequenceNoDesc(aid,cid).stream().map(BidDto::from).toList();}

    @Transactional public AuctionStateDto start(UUID cid,UUID aid){
        squadConfirmations.assertRosterMutable(cid);
        Auction a=auction(cid,aid);AuctionState s=locked(cid,aid);
        if(s.getStatus()==AuctionStatus.COMPLETED)bad("Completed auctions cannot be restarted; reset unsold players first");
        if(teams.findAllByChampionshipIdOrderByName(cid).size()<2)bad("Create at least two teams before starting the auction");
        if(s.getCurrentPlayer()==null)selectNext(a,s);if(s.getCurrentPlayer()==null)bad("Add available players before starting the auction");
        s.setStatus(AuctionStatus.RUNNING);a.setStatus(AuctionStatus.RUNNING);deadline(s);save(a,s);return publish(cid,aid,snapshot(cid,a,s));
    }
    @Transactional public AuctionStateDto pause(UUID cid,UUID aid){return changeStatus(cid,aid,AuctionStatus.PAUSED);}
    @Transactional public AuctionStateDto resume(UUID cid,UUID aid){
        squadConfirmations.assertRosterMutable(cid);
        Auction a=auction(cid,aid);AuctionState s=locked(cid,aid);if(s.getStatus()==AuctionStatus.COMPLETED)bad("Completed auctions cannot be resumed");
        if(s.getCurrentPlayer()==null)selectNext(a,s);if(s.getCurrentPlayer()==null)bad("No available players remain");
        s.setStatus(AuctionStatus.RUNNING);a.setStatus(AuctionStatus.RUNNING);deadline(s);save(a,s);return publish(cid,aid,snapshot(cid,a,s));
    }
    @Transactional public AuctionStateDto status(UUID cid,UUID aid,AuctionStatus status){if(status==null)bad("Auction status is required");return changeStatus(cid,aid,status);}
    private AuctionStateDto changeStatus(UUID cid,UUID aid,AuctionStatus status){
        squadConfirmations.assertRosterMutable(cid);
        Auction a=auction(cid,aid);AuctionState s=locked(cid,aid);s.setStatus(status);a.setStatus(status);
        if(status==AuctionStatus.RUNNING)deadline(s);else s.setBidDeadline(null);save(a,s);return publish(cid,aid,snapshot(cid,a,s));
    }

    @Transactional public AuctionStateDto bid(UUID cid,UUID aid,PlaceBidRequest req){
        squadConfirmations.assertRosterMutable(cid);
        if(req==null||req.playerId()==null||req.teamId()==null||req.amount()==null)bad("Player, team and amount are required");
        Auction a=auction(cid,aid);AuctionState s=locked(cid,aid);if(s.getStatus()!=AuctionStatus.RUNNING)bad("Auction is not running");
        Player p=players.findByIdAndChampionshipId(req.playerId(),cid).orElseThrow(()->notFound("Player"));
        Team t=teams.findByIdAndChampionshipId(req.teamId(),cid).orElseThrow(()->notFound("Team"));
        if(p.getAuction()==null||!p.getAuction().getId().equals(aid)||s.getCurrentPlayer()==null||!p.getId().equals(s.getCurrentPlayer().getId()))bad("Player is not on this auction block");
        TournamentSettings cfg=config(cid);BigDecimal increment=cfg.getBidIncrement();
        BigDecimal previous=bids.findFirstByAuctionIdAndChampionshipIdAndPlayerAndActiveTrueOrderBySequenceNoDesc(aid,cid,p).map(Bid::getAmount).orElse(p.getBasePrice().subtract(increment));
        if(req.amount().compareTo(previous.add(increment))<0)bad("Bid is below the minimum increment");validateCapacityAndPurse(t,p,req.amount(),cfg);
        long sequence=bids.maxSequence(aid,cid)+1;bids.save(Bid.builder().championship(a.getChampionship()).auction(a).player(p).team(t).amount(req.amount()).sequenceNo(sequence).active(true).build());
        deadline(s);states.save(s);broadcaster.broadcastEvent(cid,aid,"BID_PLACED",Map.of("teamName",t.getName(),"amount",req.amount(),"player",p.getFullName(),"sequence",sequence));
        return publish(cid,aid,snapshot(cid,a,s));
    }
    private void validateCapacityAndPurse(Team t,Player p,BigDecimal amount,TournamentSettings cfg){
        int male=n(t.getMaleCount()),female=n(t.getFemaleCount()),filled=male+female;if(filled>=cfg.getMaxSquadSize())bad("Team squad is full");
        if(p.getGender()==Gender.MALE&&male>=cfg.getMaxSquadSize()-cfg.getMinFemale())bad("Female roster minimum would become impossible");
        if(p.getGender()==Gender.FEMALE&&female>=cfg.getMaxSquadSize()-cfg.getMinMale())bad("Male roster minimum would become impossible");
        BigDecimal reserve=cfg.getPlayerBasePrice().multiply(BigDecimal.valueOf(cfg.getMaxSquadSize()-filled-1));
        if(t.getPurseRemaining().subtract(amount).compareTo(reserve)<0)bad("Insufficient purse reserve");
    }

    @Transactional public AuctionStateDto undo(UUID cid,UUID aid){
        squadConfirmations.assertRosterMutable(cid);
        Auction a=auction(cid,aid);AuctionState s=locked(cid,aid);if(s.getCurrentPlayer()==null)bad("No active player");Bid b=highest(cid,aid,s.getCurrentPlayer());
        b.setActive(false);bids.save(b);deadlineIfRunning(s);states.save(s);broadcaster.broadcastEvent(cid,aid,"BID_UNDONE",Map.of("amount",b.getAmount(),"teamName",b.getTeam().getName()));
        return publish(cid,aid,snapshot(cid,a,s));
    }
    @Transactional public AuctionStateDto sell(UUID cid,UUID aid){squadConfirmations.assertRosterMutable(cid);Auction a=auction(cid,aid);AuctionState s=locked(cid,aid);sellLocked(cid,a,s);return publish(cid,aid,snapshot(cid,a,s));}
    private void sellLocked(UUID cid,Auction a,AuctionState s){
        if(s.getCurrentPlayer()==null)bad("No active player");Player p=s.getCurrentPlayer();if(p.getStatus()==PlayerStatus.SOLD){advanceLocked(a,s);return;}
        Bid b=highest(cid,a.getId(),p);Team t=b.getTeam();validateCapacityAndPurse(t,p,b.getAmount(),config(cid));
        p.setTeam(t);p.setSoldPrice(b.getAmount());p.setStatus(PlayerStatus.SOLD);players.save(p);t.setPurseRemaining(t.getPurseRemaining().subtract(b.getAmount()));
        if(p.getGender()==Gender.MALE)t.setMaleCount(n(t.getMaleCount())+1);else t.setFemaleCount(n(t.getFemaleCount())+1);teams.save(t);s.setCurrentPlayer(null);states.save(s);
        broadcaster.broadcastEvent(cid,a.getId(),"PLAYER_SOLD",Map.of("player",p.getFullName(),"teamName",t.getName(),"amount",b.getAmount()));advanceLocked(a,s);
    }
    @Transactional public AuctionStateDto unsold(UUID cid,UUID aid){squadConfirmations.assertRosterMutable(cid);Auction a=auction(cid,aid);AuctionState s=locked(cid,aid);unsoldLocked(cid,a,s);return publish(cid,aid,snapshot(cid,a,s));}
    private void unsoldLocked(UUID cid,Auction a,AuctionState s){
        if(s.getCurrentPlayer()==null)bad("No active player");Player p=s.getCurrentPlayer();p.setStatus(PlayerStatus.UNSOLD);players.save(p);s.setCurrentPlayer(null);states.save(s);
        broadcaster.broadcastEvent(cid,a.getId(),"PLAYER_UNSOLD",Map.of("player",p.getFullName()));advanceLocked(a,s);
    }
    @Transactional public AuctionStateDto next(UUID cid,UUID aid){
        squadConfirmations.assertRosterMutable(cid);
        Auction a=auction(cid,aid);AuctionState s=locked(cid,aid);if(s.getCurrentPlayer()!=null&&s.getCurrentPlayer().getStatus()==PlayerStatus.ON_BLOCK){s.getCurrentPlayer().setStatus(PlayerStatus.AVAILABLE);players.save(s.getCurrentPlayer());}
        s.setCurrentPlayer(null);advanceLocked(a,s);return publish(cid,aid,snapshot(cid,a,s));
    }
    private void advanceLocked(Auction a,AuctionState s){selectNext(a,s);if(s.getCurrentPlayer()==null){s.setStatus(AuctionStatus.COMPLETED);a.setStatus(AuctionStatus.COMPLETED);s.setBidDeadline(null);}else deadlineIfRunning(s);save(a,s);}

    @Transactional public AuctionStateDto setCurrent(UUID cid,UUID aid,UUID playerId){
        squadConfirmations.assertRosterMutable(cid);
        if(playerId==null)bad("Player is required");Auction a=auction(cid,aid);AuctionState s=locked(cid,aid);Player p=players.findByIdAndChampionshipId(playerId,cid).orElseThrow(()->notFound("Player"));
        if(p.getAuction()==null||!p.getAuction().getId().equals(aid)||(p.getStatus()!=PlayerStatus.AVAILABLE&&p.getStatus()!=PlayerStatus.UNSOLD))bad("Player is not available in this auction");
        if(s.getCurrentPlayer()!=null&&s.getCurrentPlayer().getStatus()==PlayerStatus.ON_BLOCK){s.getCurrentPlayer().setStatus(PlayerStatus.AVAILABLE);players.save(s.getCurrentPlayer());}
        p.setStatus(PlayerStatus.ON_BLOCK);players.save(p);s.setCurrentPlayer(p);if(s.getStatus()==AuctionStatus.COMPLETED){s.setStatus(AuctionStatus.NOT_STARTED);a.setStatus(AuctionStatus.NOT_STARTED);}
        deadlineIfRunning(s);save(a,s);return publish(cid,aid,snapshot(cid,a,s));
    }
    @Transactional public AuctionStateDto resetUnsold(UUID cid,UUID aid){
        squadConfirmations.assertRosterMutable(cid);
        Auction a=auction(cid,aid);AuctionState s=locked(cid,aid);List<Player> list=players.findAllByAuctionIdAndChampionshipIdAndStatusOrderByAuctionOrderAsc(aid,cid,PlayerStatus.UNSOLD);list.forEach(p->p.setStatus(PlayerStatus.AVAILABLE));players.saveAll(list);
        if(s.getStatus()==AuctionStatus.COMPLETED&&!list.isEmpty()){s.setStatus(AuctionStatus.NOT_STARTED);a.setStatus(AuctionStatus.NOT_STARTED);save(a,s);}
        broadcaster.broadcastEvent(cid,aid,"UNSOLD_RESET",Map.of("count",list.size()));return publish(cid,aid,snapshot(cid,a,s));
    }
    @Transactional public PlayerDto basePrice(UUID cid,UUID aid,UUID playerId,BigDecimal amount){
        squadConfirmations.assertRosterMutable(cid);
        auction(cid,aid);locked(cid,aid);if(amount==null||amount.signum()<=0)bad("Base price must be positive");Player p=players.findByIdAndChampionshipId(playerId,cid).orElseThrow(()->notFound("Player"));
        if(p.getAuction()==null||!p.getAuction().getId().equals(aid)||p.getStatus()==PlayerStatus.SOLD)bad("Player cannot be edited in this auction");p.setBasePrice(amount);return PlayerDto.from(players.save(p));
    }
    @Transactional public Map<String,Object> coinToss(UUID cid,UUID aid,List<UUID> teamIds){
        auction(cid,aid);if(teamIds==null||teamIds.size()<2)bad("At least two teams are required");List<Team> candidates=teamIds.stream().distinct().map(id->teams.findByIdAndChampionshipId(id,cid).orElseThrow(()->notFound("Team"))).toList();
        Team team=candidates.get(new Random().nextInt(candidates.size()));Map<String,Object> result=Map.of("winnerTeamId",team.getId().toString(),"winnerTeamName",team.getName());broadcaster.broadcastEvent(cid,aid,"COIN_TOSS",result);return result;
    }

    @Scheduled(fixedDelay=1000L) public void timerSweep(){for(UUID id:states.findExpiredStateIds(Instant.now())){try{transactions.executeWithoutResult(x->processExpired(id));}catch(Exception ex){log.error("Unable to process expired auction state {}",id,ex);}}}
    private void processExpired(UUID id){AuctionState s=states.findByIdForUpdate(id).orElse(null);if(s==null||s.getStatus()!=AuctionStatus.RUNNING||s.getCurrentPlayer()==null||s.getBidDeadline()==null||Instant.now().isBefore(s.getBidDeadline()))return;
        Auction a=s.getAuction();UUID cid=s.getChampionship().getId();if(bids.findFirstByAuctionIdAndChampionshipIdAndPlayerAndActiveTrueOrderBySequenceNoDesc(a.getId(),cid,s.getCurrentPlayer()).isPresent())sellLocked(cid,a,s);else unsoldLocked(cid,a,s);publish(cid,a.getId(),snapshot(cid,a,s));}

    private AuctionStateDto snapshot(UUID cid,Auction a,AuctionState s){
        List<BidDto> history=bids.findTop50ByAuctionIdAndChampionshipIdOrderBySequenceNoDesc(a.getId(),cid).stream().map(BidDto::from).toList();BidDto top=s.getCurrentPlayer()==null?null:bids.findFirstByAuctionIdAndChampionshipIdAndPlayerAndActiveTrueOrderBySequenceNoDesc(a.getId(),cid,s.getCurrentPlayer()).map(BidDto::from).orElse(null);
        List<TeamDto> teamDtos=teams.findAllByChampionshipIdOrderByName(cid).stream().map(TeamDto::from).toList();int remaining=players.findAllByAuctionIdAndChampionshipIdAndStatusOrderByAuctionOrderAsc(a.getId(),cid,PlayerStatus.AVAILABLE).size();TournamentSettings cfg=config(cid);
        return new AuctionStateDto(s.getStatus(),s.getCurrentPlayer()==null?null:PlayerDto.from(s.getCurrentPlayer()),top,history,teamDtos,remaining,s.getBidDeadline(),s.getTimerSeconds(),cfg.getBidIncrement(),cfg.getMaxSquadSize(),cfg.getMinMale(),cfg.getMinFemale());
    }
    private void selectNext(Auction a,AuctionState s){List<Player> available=players.findAllByAuctionIdAndChampionshipIdAndStatusOrderByAuctionOrderAsc(a.getId(),s.getChampionship().getId(),PlayerStatus.AVAILABLE);if(available.isEmpty()){s.setCurrentPlayer(null);return;}Player p=available.get(0);p.setStatus(PlayerStatus.ON_BLOCK);players.save(p);s.setCurrentPlayer(p);}
    private void deadline(AuctionState s){int seconds=s.getTimerSeconds()==null?30:s.getTimerSeconds();s.setTimerSeconds(seconds);s.setBidDeadline(Instant.now().plusSeconds(seconds));}
    private void deadlineIfRunning(AuctionState s){if(s.getStatus()==AuctionStatus.RUNNING)deadline(s);else s.setBidDeadline(null);}
    private void save(Auction a,AuctionState s){auctions.save(a);states.save(s);}private Auction auction(UUID cid,UUID aid){return auctions.findByIdAndChampionshipId(aid,cid).orElseThrow(()->notFound("Auction"));}
    private AuctionState auctionState(UUID cid,UUID aid){return states.findByAuctionIdAndChampionshipId(aid,cid).orElseThrow(()->notFound("Auction state"));}private AuctionState locked(UUID cid,UUID aid){return states.findByAuctionIdForUpdate(aid,cid).orElseThrow(()->notFound("Auction state"));}
    private TournamentSettings config(UUID cid){return settings.findByChampionshipId(cid).orElseThrow(()->notFound("Tournament settings"));}private Bid highest(UUID cid,UUID aid,Player p){return bids.findFirstByAuctionIdAndChampionshipIdAndPlayerAndActiveTrueOrderBySequenceNoDesc(aid,cid,p).orElseThrow(()->notFound("Active bid"));}
    private AuctionStateDto publish(UUID cid,UUID aid,AuctionStateDto dto){broadcaster.broadcastState(cid,aid,dto);return dto;}private int n(Integer v){return v==null?0:v;}private void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private ResponseStatusException notFound(String v){return new ResponseStatusException(HttpStatus.NOT_FOUND,v+" not found");}
}
