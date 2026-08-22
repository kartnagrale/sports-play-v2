package com.neml.badminton.service;

import com.neml.badminton.dto.MatchDtos.*;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.websocket.AuctionBroadcaster;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class MatchService {
    private static final FormatType[] BADMINTON_FORMATS={FormatType.MENS_SINGLES,FormatType.WOMENS_SINGLES,FormatType.MENS_DOUBLES,FormatType.MIXED_DOUBLES,FormatType.MENS_DOUBLES_TWO};
    private final MatchRepository matches;private final MatchFormatRepository formats;private final TeamRepository teams;
    private final PlayerRepository players;private final AuctionBroadcaster broadcaster;private final ChampionshipRepository championships;
    private final TournamentSettingsRepository settings;

    public MatchService(MatchRepository matches,MatchFormatRepository formats,TeamRepository teams,PlayerRepository players,
            AuctionBroadcaster broadcaster,ChampionshipRepository championships,TournamentSettingsRepository settings){
        this.matches=matches;this.formats=formats;this.teams=teams;this.players=players;this.broadcaster=broadcaster;this.championships=championships;this.settings=settings;
    }

    @Transactional public List<MatchDto> listAll(UUID cid){return matches.findAllByChampionshipIdOrderByMatchNumberAsc(cid).stream().map(MatchDto::from).toList();}
    @Transactional public MatchDto get(UUID cid,UUID id){return MatchDto.from(match(cid,id));}

    @Transactional public MatchDto create(UUID cid,CreateMatchRequest req){
        Championship championship=championships.findByIdForUpdate(cid).orElseThrow(()->notFound("Championship"));
        Team a=teams.findByIdAndChampionshipId(req.teamAId(),cid).orElseThrow(()->notFound("Team A"));
        Team b=teams.findByIdAndChampionshipId(req.teamBId(),cid).orElseThrow(()->notFound("Team B"));
        if(a.getId().equals(b.getId()))bad("Teams must be different");
        Match m=matches.save(Match.builder().championship(championship).teamA(a).teamB(b).scheduledAt(req.scheduledAt())
                .status(MatchStatus.SCHEDULED).teamAFormatWins(0).teamBFormatWins(0).matchNumber(matches.maxMatchNumber(cid)+1).venue(req.venue()).build());
        if("BADMINTON".equalsIgnoreCase(championship.getSportType()))for(int i=0;i<BADMINTON_FORMATS.length;i++){
            MatchFormat f=formats.save(MatchFormat.builder().match(m).formatType(BADMINTON_FORMATS[i]).formatOrder(i+1).scoreA(0).scoreB(0).completed(false).build());m.getFormats().add(f);
        }
        broadcaster.broadcastMatch(cid,"MATCH_CREATED",event(m));return MatchDto.from(match(cid,m.getId()));
    }

    @Transactional public MatchDto assignPlayers(UUID cid,UUID formatId,AssignPlayersRequest req){
        MatchFormat f=format(cid,formatId);if(Boolean.TRUE.equals(f.getCompleted()))bad("Format already completed");Match m=f.getMatch();
        Set<UUID> usedA=new HashSet<>(),usedB=new HashSet<>();for(MatchFormat other:m.getFormats()){if(other.getId().equals(f.getId()))continue;other.getSideAPlayers().forEach(p->usedA.add(p.getId()));other.getSideBPlayers().forEach(p->usedB.add(p.getId()));}
        if(req.sideAPlayerIds().stream().anyMatch(usedA::contains)||req.sideBPlayerIds().stream().anyMatch(usedB::contains))bad("A player can participate in only one format per match");
        List<Player> a=loadPlayers(cid,req.sideAPlayerIds()),b=loadPlayers(cid,req.sideBPlayerIds());
        if(a.stream().anyMatch(p->p.getTeam()==null||!p.getTeam().getId().equals(m.getTeamA().getId())))bad("Every Team A player must belong to Team A");
        if(b.stream().anyMatch(p->p.getTeam()==null||!p.getTeam().getId().equals(m.getTeamB().getId())))bad("Every Team B player must belong to Team B");
        validateComposition(f.getFormatType(),a,b);f.getSideAPlayers().clear();f.getSideAPlayers().addAll(a);f.getSideBPlayers().clear();f.getSideBPlayers().addAll(b);formats.save(f);
        if(m.getStatus()==MatchStatus.SCHEDULED){m.setStatus(MatchStatus.LIVE);matches.save(m);}broadcaster.broadcastMatch(cid,"FORMAT_ASSIGNED",event(m));return MatchDto.from(match(cid,m.getId()));
    }

    @Transactional public MatchDto reportLiveScore(UUID cid,UUID formatId,ReportFormatResultRequest req){
        MatchFormat f=format(cid,formatId);ensureAssigned(f);if(Boolean.TRUE.equals(f.getCompleted()))bad("Completed formats cannot be edited");f.setScoreA(req.scoreA());f.setScoreB(req.scoreB());formats.save(f);Match m=f.getMatch();
        if(m.getStatus()==MatchStatus.SCHEDULED){m.setStatus(MatchStatus.LIVE);matches.save(m);}broadcaster.broadcastMatch(cid,"FORMAT_LIVE_SCORE",event(m));return MatchDto.from(match(cid,m.getId()));
    }

    @Transactional public MatchDto reportFormatResult(UUID cid,UUID formatId,ReportFormatResultRequest req){
        MatchFormat f=format(cid,formatId);ensureAssigned(f);if(Boolean.TRUE.equals(f.getCompleted()))bad("Format result is already final");if(req.scoreA().equals(req.scoreB()))bad("Scores cannot be equal");Match m=f.getMatch();
        f.setScoreA(req.scoreA());f.setScoreB(req.scoreB());f.setWinnerTeam(req.scoreA()>req.scoreB()?m.getTeamA():m.getTeamB());f.setCompleted(true);formats.save(f);
        int aWins=0,bWins=0,completed=0;for(MatchFormat item:m.getFormats())if(Boolean.TRUE.equals(item.getCompleted())){completed++;if(item.getWinnerTeam()!=null&&item.getWinnerTeam().getId().equals(m.getTeamA().getId()))aWins++;else if(item.getWinnerTeam()!=null&&item.getWinnerTeam().getId().equals(m.getTeamB().getId()))bWins++;}
        m.setTeamAFormatWins(aWins);m.setTeamBFormatWins(bWins);if(m.getStatus()==MatchStatus.SCHEDULED)m.setStatus(MatchStatus.LIVE);
        if(completed==m.getFormats().size()){m.setStatus(MatchStatus.COMPLETED);Team winner=aWins>bWins?m.getTeamA():m.getTeamB();m.setWinnerTeam(winner);int points=settings.findByChampionshipId(cid).map(TournamentSettings::getPointsPerWin).orElse(3);winner.setMatchPoints((winner.getMatchPoints()==null?0:winner.getMatchPoints())+points);teams.save(winner);}
        matches.save(m);broadcaster.broadcastMatch(cid,"FORMAT_RESULT",event(m));return MatchDto.from(match(cid,m.getId()));
    }

    @Transactional public void deleteMatch(UUID cid,UUID id){Match m=match(cid,id);if(m.getStatus()==MatchStatus.COMPLETED&&m.getWinnerTeam()!=null){int points=settings.findByChampionshipId(cid).map(TournamentSettings::getPointsPerWin).orElse(3);Team winner=m.getWinnerTeam();winner.setMatchPoints(Math.max(0,(winner.getMatchPoints()==null?0:winner.getMatchPoints())-points));teams.save(winner);}matches.delete(m);broadcaster.broadcastMatch(cid,"MATCH_DELETED",Map.of("id",id.toString()));}

    private List<Player> loadPlayers(UUID cid,List<UUID> ids){if(ids==null)bad("Player selection is required");if(ids.stream().distinct().count()!=ids.size())bad("A player cannot occupy two slots in one format");return ids.stream().map(id->players.findByIdAndChampionshipId(id,cid).orElseThrow(()->notFound("Player"))).toList();}
    private void ensureAssigned(MatchFormat f){if(f.getSideAPlayers().isEmpty()||f.getSideBPlayers().isEmpty())bad("Assign players before reporting a score");}
    private void validateComposition(FormatType type,List<Player> a,List<Player> b){int expected=switch(type){case MENS_SINGLES,WOMENS_SINGLES->1;default->2;};if(a.size()!=expected||b.size()!=expected)bad(type+" requires "+expected+" player(s) per side");
        switch(type){case MENS_SINGLES,MENS_DOUBLES,MENS_DOUBLES_TWO->{if(anyNot(a,Gender.MALE)||anyNot(b,Gender.MALE))bad("Only male players are allowed for "+type);}case WOMENS_SINGLES->{if(anyNot(a,Gender.FEMALE)||anyNot(b,Gender.FEMALE))bad("Only female players are allowed for "+type);}case MIXED_DOUBLES->{if(!mixed(a)||!mixed(b))bad("Mixed doubles requires one male and one female per side");}}}
    private boolean anyNot(List<Player> list,Gender gender){return list.stream().anyMatch(p->p.getGender()!=gender);}private boolean mixed(List<Player> list){return list.size()==2&&list.stream().filter(p->p.getGender()==Gender.MALE).count()==1&&list.stream().filter(p->p.getGender()==Gender.FEMALE).count()==1;}
    private Match match(UUID cid,UUID id){return matches.findByIdAndChampionshipId(id,cid).orElseThrow(()->notFound("Match"));}private MatchFormat format(UUID cid,UUID id){return formats.findByIdAndMatchChampionshipId(id,cid).orElseThrow(()->notFound("Format"));}
    private Map<String,Object> event(Match m){return Map.of("matchId",m.getId().toString(),"matchNumber",m.getMatchNumber(),"status",m.getStatus().name(),"teamA",m.getTeamA().getName(),"teamB",m.getTeamB().getName());}
    private void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private ResponseStatusException notFound(String value){return new ResponseStatusException(HttpStatus.NOT_FOUND,value+" not found");}
}
