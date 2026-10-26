package com.neml.badminton.service;

import com.neml.badminton.competition.*;
import com.neml.badminton.dto.MatchDtos.*;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.websocket.AuctionBroadcaster;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class MatchService {
    private final MatchRepository matches;
    private final MatchFormatRepository formats;
    private final TeamRepository teams;
    private final PlayerRepository players;
    private final AuctionBroadcaster broadcaster;
    private final ChampionshipRepository championships;
    private final CompetitionConfigurationRepository configurations;
    private final CompetitionStageRepository stages;
    private final SquadConfirmationService squads;
    private final SportEngineRegistry engines;
    private final CompetitionService competition;
    private final ConfigurationSectionService configurationSections;
    private final BadmintonConfigurationRepository badmintonConfigurations;

    public MatchService(MatchRepository matches, MatchFormatRepository formats, TeamRepository teams,
            PlayerRepository players, AuctionBroadcaster broadcaster, ChampionshipRepository championships,
            TournamentSettingsRepository ignoredLegacySettings, SquadConfirmationService squads,
            CompetitionConfigurationRepository configurations, CompetitionStageRepository stages,
            SportEngineRegistry engines,
            CompetitionService competition,ConfigurationSectionService configurationSections,BadmintonConfigurationRepository badmintonConfigurations) {
        this.matches=matches; this.formats=formats; this.teams=teams; this.players=players;
        this.broadcaster=broadcaster; this.championships=championships; this.squads=squads;
        this.configurations=configurations; this.stages=stages; this.engines=engines; this.competition=competition;
        this.configurationSections=configurationSections;
        this.badmintonConfigurations=badmintonConfigurations;
    }

    @Transactional public List<MatchDto> listAll(UUID cid) {
        return matches.findAllByChampionshipIdOrderByMatchNumberAsc(cid).stream().map(MatchDto::from).toList();
    }
    @Transactional public MatchDto get(UUID cid,UUID id) { return MatchDto.from(match(cid,id)); }

    /** Backward-compatible manual league scheduling. Generated fixture workflows should use CompetitionService. */
    @Transactional public MatchDto create(UUID cid,CreateMatchRequest request) {
        squads.assertLeagueReady(cid);
        Championship championship=championships.findByIdForUpdate(cid).orElseThrow(()->notFound("Championship"));
        SportCompetitionEngine engine=engine(championship);
        Team a=teams.findByIdAndChampionshipId(request.teamAId(),cid).orElseThrow(()->notFound("Team A"));
        Team b=teams.findByIdAndChampionshipId(request.teamBId(),cid).orElseThrow(()->notFound("Team B"));
        if(a.getId().equals(b.getId())) bad("Teams must be different");
        if(matches.existsPair(cid,CompetitionStageType.LEAGUE,a.getId(),b.getId()))
            conflict("A league fixture for these teams already exists");
        CompetitionStage leagueStage=stages.findForUpdate(cid,CompetitionStageType.LEAGUE).orElseGet(()->
                stages.save(CompetitionStage.builder().championship(championship)
                        .stageType(CompetitionStageType.LEAGUE).stageOrder(1)
                        .status(CompetitionStageStatus.ACTIVE).startedAt(Instant.now()).build()));
        Match fixture=matches.save(Match.builder().championship(championship)
                .competitionStage(leagueStage).stageChampionshipId(cid)
                .stageType(CompetitionStageType.LEAGUE)
                .roundNumber(1).legNumber(1).fixtureKey("MANUAL-"+UUID.randomUUID())
                .teamA(a).teamB(b).scheduledAt(request.scheduledAt()).status(MatchStatus.SCHEDULED)
                .teamAFormatWins(0).teamBFormatWins(0).teamAPoints(0).teamBPoints(0).resultRevision(0)
                .matchNumber(matches.maxMatchNumber(cid)+1).venue(request.venue()).scoreData("{}").build());
        CompetitionConfiguration competitionConfiguration=configurations.findByChampionshipId(cid).orElseThrow(()->notFound("Competition configuration"));
        for(var item:engine.fixtureComponents(competitionConfiguration)) {
            MatchFormat format=formats.save(MatchFormat.builder().championship(championship).match(fixture).formatType(item.formatType())
                    .formatOrder(item.order()).displayLabel(item.label()).shortLabel(item.shortLabel()).playersPerSide(item.playersPerSide()).genderRule(item.genderRule()).scoreA(0).scoreB(0).completed(false).build());
            fixture.getFormats().add(format);
        }
        broadcaster.broadcastMatch(cid,"MATCH_CREATED",event(fixture));
        return MatchDto.from(match(cid,fixture.getId()));
    }

    @Transactional public MatchDto assignPlayers(UUID cid,UUID formatId,AssignPlayersRequest request) {
        MatchFormat format=lockedFormat(cid,formatId);
        if(Boolean.TRUE.equals(format.getCompleted())) conflict("Completed rubber assignments cannot be edited");
        Match fixture=format.getMatch();
        boolean scoringStarted=fixture.getFormats().stream().anyMatch(item->Boolean.TRUE.equals(item.getCompleted())||item.getScoreA()>0||item.getScoreB()>0);
        if(scoringStarted&&!engine(fixture.getChampionship()).lineupChangesAfterScoringAllowed(cid)) conflict("Lineup changes are locked after fixture scoring starts");
        squads.assertTeamLocked(cid,fixture.getTeamA().getId());
        squads.assertTeamLocked(cid,fixture.getTeamB().getId());
        List<Player> sideA=loadPlayers(cid,request.sideAPlayerIds());
        List<Player> sideB=loadPlayers(cid,request.sideBPlayerIds());
        validateOwnershipAndEligibility(sideA,fixture.getTeamA(),"Team A");
        validateOwnershipAndEligibility(sideB,fixture.getTeamB(),"Team B");
        engine(fixture.getChampionship()).validateLineup(format,sideA,sideB);
        format.getSideAPlayers().clear(); format.getSideAPlayers().addAll(sideA);
        format.getSideBPlayers().clear(); format.getSideBPlayers().addAll(sideB); formats.save(format);
        if(fixture.getStatus()==MatchStatus.SCHEDULED) {fixture.setStatus(MatchStatus.LIVE);matches.save(fixture);}
        broadcaster.broadcastMatch(cid,"FORMAT_ASSIGNED",event(fixture));
        return MatchDto.from(match(cid,fixture.getId()));
    }

    @Transactional public MatchDto reportLiveScore(UUID cid,UUID formatId,ReportFormatResultRequest request) {
        configurationSections.lock(cid,"SPORT_RULES",null);
        MatchFormat format=lockedFormat(cid,formatId); ensureAssigned(format);
        if(Boolean.TRUE.equals(format.getCompleted())) conflict("Completed rubbers cannot receive live scores");
        engine(format.getMatch().getChampionship()).validateScore(format,request.scoreA(),request.scoreB(),false);
        format.setScoreA(request.scoreA()); format.setScoreB(request.scoreB()); formats.save(format);
        Match fixture=format.getMatch();
        if(fixture.getStatus()==MatchStatus.SCHEDULED) {fixture.setStatus(MatchStatus.LIVE);matches.save(fixture);}
        broadcaster.broadcastMatch(cid,"FORMAT_LIVE_SCORE",event(fixture));
        return MatchDto.from(match(cid,fixture.getId()));
    }

    @Transactional public MatchDto reportFormatResult(UUID cid,UUID formatId,ReportFormatResultRequest request,User actor) {
        configurationSections.lock(cid,"SPORT_RULES",actor);
        MatchFormat format=lockedFormat(cid,formatId); ensureAssigned(format);
        if(Boolean.TRUE.equals(format.getCompleted())) {
            if(Objects.equals(format.getScoreA(),request.scoreA())&&Objects.equals(format.getScoreB(),request.scoreB()))
                return MatchDto.from(format.getMatch());
            conflict("This result is final; use the correction action to change it");
        }
        return applyResult(cid,format,request,actor,"RUBBER_RESULT_CONFIRMED");
    }

    @Transactional public MatchDto correctFormatResult(UUID cid,UUID formatId,ReportFormatResultRequest request,User actor) {
        MatchFormat format=lockedFormat(cid,formatId); ensureAssigned(format);
        if(!Boolean.TRUE.equals(format.getCompleted())) bad("Only completed rubber results require correction");
        Match fixture=format.getMatch();
        if(competition.hasDownstreamFixture(cid,fixture.getStageType()))
            conflict("Delete or resolve downstream playoff fixtures before correcting this result");
        return applyResult(cid,format,request,actor,"RUBBER_RESULT_CORRECTED");
    }

    private MatchDto applyResult(UUID cid,MatchFormat format,ReportFormatResultRequest request,User actor,String auditType) {
        Match fixture=format.getMatch();
        Map<String,Object> before=Map.of("scoreA",format.getScoreA(),"scoreB",format.getScoreB(),
                "completed",format.getCompleted());
        SportCompetitionEngine engine=engine(fixture.getChampionship());
        engine.validateScore(format,request.scoreA(),request.scoreB(),true);
        format.setScoreA(request.scoreA()); format.setScoreB(request.scoreB());
        format.setWinnerTeam(request.scoreA()>request.scoreB()?fixture.getTeamA():fixture.getTeamB());
        format.setCompleted(true); formats.save(format);
        SportCompetitionEngine.MatchOutcome outcome=engine.calculateMatchOutcome(fixture);
        fixture.setTeamAFormatWins(outcome.sideAWins()); fixture.setTeamBFormatWins(outcome.sideBWins());
        fixture.setResultRevision((fixture.getResultRevision()==null?0:fixture.getResultRevision())+1);
        if(outcome.completed()) {
            fixture.setStatus(MatchStatus.COMPLETED); fixture.setWinnerTeam(outcome.winner());
            CompetitionConfiguration config=configurations.findByChampionshipId(cid)
                    .orElseThrow(()->notFound("Competition configuration"));
            boolean tied=outcome.winner()==null;boolean aWon=!tied&&outcome.winner().getId().equals(fixture.getTeamA().getId());
            fixture.setTeamAPoints(fixture.getStageType()==CompetitionStageType.LEAGUE?(tied?config.getPointsPerTie():(aWon?config.getPointsPerWin():config.getPointsPerLoss())):0);
            fixture.setTeamBPoints(fixture.getStageType()==CompetitionStageType.LEAGUE?(tied?config.getPointsPerTie():(aWon?config.getPointsPerLoss():config.getPointsPerWin())):0);
        } else {
            fixture.setStatus(MatchStatus.LIVE); fixture.setWinnerTeam(null);
            fixture.setTeamAPoints(0); fixture.setTeamBPoints(0);
        }
        matches.save(fixture);
        competition.recordResultAudit(fixture,actor,auditType,Map.of("before",before,
                "scoreA",request.scoreA(),"scoreB",request.scoreB(),"fixtureStatus",fixture.getStatus()));
        broadcaster.broadcastMatch(cid,"FORMAT_RESULT",event(fixture));
        if(fixture.getStatus()==MatchStatus.COMPLETED) competition.advanceAfterCompletion(cid,fixture.getId(),actor);
        return MatchDto.from(match(cid,fixture.getId()));
    }

    @Transactional public void deleteMatch(UUID cid,UUID id,User actor) {
        Match fixture=matches.findByIdAndChampionshipIdForUpdate(id,cid).orElseThrow(()->notFound("Fixture"));
        if(competition.hasDownstreamFixture(cid,fixture.getStageType()))
            conflict("This fixture has downstream playoff state and cannot be deleted");
        competition.recordResultAudit(fixture,actor,"FIXTURE_DELETED",Map.of(
                "fixtureKey",fixture.getFixtureKey(),"stage",fixture.getStageType(),"status",fixture.getStatus()));
        reverseLifecycleAfterDeletion(cid,fixture);
        matches.delete(fixture);
        broadcaster.broadcastMatch(cid,"MATCH_DELETED",Map.of("id",id.toString()));
    }

    @Transactional public MatchDto administrativeFixtureResult(UUID cid,UUID id,AdministrativeFixtureResultRequest request,User actor){
        Match fixture=matches.findByIdAndChampionshipIdForUpdate(id,cid).orElseThrow(()->notFound("Fixture"));
        if(!"BADMINTON".equalsIgnoreCase(fixture.getChampionship().getSportType()))bad("This administrative decision is only valid for badminton team-match ties");
        BadmintonConfiguration rules=badmintonConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(cid).orElseThrow(()->notFound("Badminton configuration"));
        if(!"ADMINISTRATIVE_WIN".equals(rules.getTieHandling()))conflict("Administrative tie decisions are disabled by championship policy");
        if(fixture.getFormats().isEmpty()||fixture.getFormats().stream().anyMatch(format->!Boolean.TRUE.equals(format.getCompleted())))conflict("Complete every rubber before deciding a tied team match");
        int a=fixture.getFormats().stream().mapToInt(format->format.getWinnerTeam()!=null&&format.getWinnerTeam().getId().equals(fixture.getTeamA().getId())?1:0).sum();
        int b=fixture.getFormats().stream().mapToInt(format->format.getWinnerTeam()!=null&&format.getWinnerTeam().getId().equals(fixture.getTeamB().getId())?1:0).sum();
        if(a!=b)conflict("Administrative tie decisions require an even-rubber tied result");
        Team winner=teams.findByIdAndChampionshipId(request.winnerTeamId(),cid).orElseThrow(()->notFound("Winner team"));
        if(!winner.getId().equals(fixture.getTeamA().getId())&&!winner.getId().equals(fixture.getTeamB().getId()))bad("Winner must participate in this fixture");
        Map<String,Object> before=Map.of("status",fixture.getStatus(),"teamAWins",a,"teamBWins",b);
        fixture.setWinnerTeam(winner);fixture.setStatus(MatchStatus.COMPLETED);fixture.setTeamAFormatWins(a);fixture.setTeamBFormatWins(b);fixture.setResultRevision((fixture.getResultRevision()==null?0:fixture.getResultRevision())+1);
        CompetitionConfiguration common=configurations.findByChampionshipId(cid).orElseThrow(()->notFound("Competition configuration"));boolean aWon=winner.getId().equals(fixture.getTeamA().getId());fixture.setTeamAPoints(fixture.getStageType()==CompetitionStageType.LEAGUE?(aWon?common.getPointsPerWin():common.getPointsPerLoss()):0);fixture.setTeamBPoints(fixture.getStageType()==CompetitionStageType.LEAGUE?(aWon?common.getPointsPerLoss():common.getPointsPerWin()):0);matches.save(fixture);
        competition.recordResultAudit(fixture,actor,"BADMINTON_ADMINISTRATIVE_RESULT",Map.of("before",before,"afterWinnerTeamId",winner.getId(),"reason",request.reason()));competition.advanceAfterCompletion(cid,id,actor);broadcaster.broadcastMatch(cid,"MATCH_RESULT",event(fixture));return MatchDto.from(match(cid,id));
    }

    private void reverseLifecycleAfterDeletion(UUID cid,Match fixture) {
        if(fixture.getStatus()!=MatchStatus.COMPLETED) return;
        stages.findForUpdate(cid,fixture.getStageType()).ifPresent(stage->{
            stage.setStatus(CompetitionStageStatus.ACTIVE);
            stage.setCompletedAt(null);
            stages.save(stage);
        });
        if(fixture.getStageType()!=CompetitionStageType.FINAL) return;
        CompetitionConfiguration configuration=configurations.findByChampionshipIdForUpdate(cid)
                .orElseThrow(()->notFound("Competition configuration"));
        configuration.setChampion(null);
        configuration.setRunnerUp(null);
        configurations.save(configuration);
        Championship championship=fixture.getChampionship();
        championship.setStatus(ChampionshipStatus.ACTIVE);
        championship.setLifecyclePhase(ChampionshipPhase.FINAL);
        championships.save(championship);
    }

    private void validateOwnershipAndEligibility(List<Player> selected,Team expected,String label) {
        if(selected.stream().anyMatch(player->player.getTeam()==null||!player.getTeam().getId().equals(expected.getId())))
            bad("Every "+label+" player must belong to "+expected.getName());
        if(selected.stream().anyMatch(player->player.getStatus()!=PlayerStatus.SOLD))
            bad("Only eligible auctioned players from the locked squad may be selected");
    }
    private List<Player> loadPlayers(UUID cid,List<UUID> ids) {
        if(ids==null) bad("Player selection is required");
        if(ids.stream().distinct().count()!=ids.size()) bad("A player cannot occupy multiple slots");
        return ids.stream().map(id->players.findByIdAndChampionshipId(id,cid).orElseThrow(()->notFound("Player"))).toList();
    }
    private void ensureAssigned(MatchFormat format) {
        if(format.getSideAPlayers().isEmpty()||format.getSideBPlayers().isEmpty())
            bad("Assign players before reporting a score");
    }
    private SportCompetitionEngine engine(Championship championship) {return engines.require(championship.getSportType());}
    private Match match(UUID cid,UUID id) {return matches.findByIdAndChampionshipId(id,cid).orElseThrow(()->notFound("Match"));}
    private MatchFormat lockedFormat(UUID cid,UUID id) {return formats.findByIdAndChampionshipIdForUpdate(id,cid).orElseThrow(()->notFound("Rubber"));}
    private Map<String,Object> event(Match fixture) {return Map.of("matchId",fixture.getId().toString(),
            "matchNumber",fixture.getMatchNumber(),"stage",fixture.getStageType().name(),"status",fixture.getStatus().name(),
            "teamA",fixture.getTeamA().getName(),"teamB",fixture.getTeamB().getName());}
    private void bad(String message) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private void conflict(String message) {throw new ResponseStatusException(HttpStatus.CONFLICT,message);}
    private ResponseStatusException notFound(String value) {return new ResponseStatusException(HttpStatus.NOT_FOUND,value+" not found");}
}
