package com.neml.badminton.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neml.badminton.competition.*;
import com.neml.badminton.dto.CompetitionDtos;
import com.neml.badminton.dto.CompetitionDtos.*;
import com.neml.badminton.dto.MatchDtos.*;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CompetitionService {
    private final ChampionshipRepository championships;
    private final ChampionshipRoleRepository roles;
    private final CompetitionConfigurationRepository configs;
    private final CompetitionStageRepository stages;
    private final CompetitionQualificationRepository qualifications;
    private final CompetitionAuditEventRepository audits;
    private final TeamRepository teams;
    private final PlayerRepository players;
    private final MatchRepository fixtures;
    private final MatchFormatRepository formats;
    private final TournamentSettingsRepository settings;
    private final AuctionRepository auctions;
    private final SquadConfirmationService squads;
    private final SportEngineRegistry engines;
    private final ObjectMapper json;
    private final ConfigurationSectionService configurationSections;
    private final AuctionConfigurationRepository auctionConfigurations;

    public CompetitionService(ChampionshipRepository championships, ChampionshipRoleRepository roles,
            CompetitionConfigurationRepository configs, CompetitionStageRepository stages,
            CompetitionQualificationRepository qualifications, CompetitionAuditEventRepository audits,
            TeamRepository teams, PlayerRepository players, MatchRepository fixtures,
            MatchFormatRepository formats, TournamentSettingsRepository settings,AuctionRepository auctions,
            SquadConfirmationService squads, SportEngineRegistry engines, ObjectMapper json,ConfigurationSectionService configurationSections,AuctionConfigurationRepository auctionConfigurations) {
        this.championships=championships; this.roles=roles; this.configs=configs; this.stages=stages;
        this.qualifications=qualifications; this.audits=audits; this.teams=teams; this.players=players;
        this.fixtures=fixtures; this.formats=formats; this.settings=settings; this.auctions=auctions;this.squads=squads;
        this.engines=engines; this.json=json;this.configurationSections=configurationSections;
        this.auctionConfigurations=auctionConfigurations;
    }

    @Transactional public CompetitionOverviewDto overview(UUID cid) {
        return overview(championship(cid), config(cid), fixtures.findAllByChampionshipIdOrderByMatchNumberAsc(cid));
    }
    @Transactional public CompetitionConfigurationDto configuration(UUID cid) {
        championship(cid); return CompetitionDtos.from(config(cid));
    }
    @Transactional public SportCapabilitiesDto capabilities(UUID cid) {
        Championship championship=championship(cid);
        if(!engines.supports(championship.getSportType()))
            return new SportCapabilitiesDto(SportEngineRegistry.normalize(championship.getSportType()),List.of(),
                    List.of("UPCOMING_FIXTURES"),Map.of("fixture","Match","standings","Standings"));
        SportCompetitionEngine engine=engine(championship);
        return new SportCapabilitiesDto(engine.sportCode(),engine.fixtureComponents(),engine.supportedWidgets(),engine.terminology());
    }

    @Transactional public CompetitionConfigurationDto configure(UUID cid,CompetitionConfigurationRequest request,User actor) {
        Championship championship=lockedChampionship(cid);
        if(fixtures.existsByChampionshipIdAndStageType(cid,CompetitionStageType.LEAGUE))
            conflict("League configuration is frozen after fixture generation");
        CompetitionConfiguration c=lockedConfig(cid);
        CompetitionConfigurationDto before=CompetitionDtos.from(c);
        if(request.expectedConfigurationVersion()!=null&&!request.expectedConfigurationVersion().equals(c.getConfigurationVersion()))
            conflict("Configuration version conflict: reload the latest configuration before saving");
        int existingTeams=teams.findAllByChampionshipIdOrderByName(cid).size();
        if(existingTeams>0&&((request.configuredTeamCount()!=null&&!request.configuredTeamCount().equals(c.getConfiguredTeamCount()))||(request.minimumTeamCount()!=null&&!request.minimumTeamCount().equals(c.getMinimumTeamCount()))||(request.maximumTeamCount()!=null&&!request.maximumTeamCount().equals(c.getMaximumTeamCount()))))
            conflict("Team-count configuration is locked after team provisioning begins");
        var squadState=squads.summary(cid);
        if((squadState.confirmedTeams()>0||squadState.lockedTeams()>0)&&((request.minimumRosterSize()!=null&&!request.minimumRosterSize().equals(c.getMinimumRosterSize()))||(request.maximumRosterSize()!=null&&!request.maximumRosterSize().equals(c.getMaximumRosterSize()))||(request.playingSideSize()!=null&&!request.playingSideSize().equals(c.getPlayingSideSize()))||(request.maximumSubstitutes()!=null&&!request.maximumSubstitutes().equals(c.getMaximumSubstitutes()))))
            conflict("Roster and playing-side limits are locked after squad submission begins");
        int existingLargestRoster=players.findAllByChampionshipIdOrderByAuctionOrderAsc(cid).stream()
                .filter(player->player.getTeam()!=null).collect(Collectors.groupingBy(p->p.getTeam().getId(),Collectors.counting()))
                .values().stream().mapToInt(Long::intValue).max().orElse(0);
        if(request.configuredTeamCount()!=null)c.setConfiguredTeamCount(request.configuredTeamCount());
        if(request.minimumTeamCount()!=null)c.setMinimumTeamCount(request.minimumTeamCount());
        if(request.maximumTeamCount()!=null)c.setMaximumTeamCount(request.maximumTeamCount());
        if(request.minimumRosterSize()!=null)c.setMinimumRosterSize(request.minimumRosterSize());
        if(request.maximumRosterSize()!=null)c.setMaximumRosterSize(request.maximumRosterSize());
        if(request.playingSideSize()!=null)c.setPlayingSideSize(request.playingSideSize());
        if(request.maximumSubstitutes()!=null)c.setMaximumSubstitutes(request.maximumSubstitutes());
        if(request.numberOfLegs()!=null)c.setNumberOfLegs(request.numberOfLegs());
        if(request.playoffFormat()!=null)c.setPlayoffFormat(request.playoffFormat());
        if(request.timezone()!=null&&!request.timezone().isBlank())c.setTimezone(request.timezone().trim());
        if(request.presetCode()!=null)c.setPresetCode(request.presetCode().trim());
        c.setRoundRobinMode(request.roundRobinMode()); c.setPointsPerWin(request.pointsPerWin());
        c.setPointsPerLoss(request.pointsPerLoss()); c.setTieBreakerPriority(join(request.tieBreakerPriority(),","));
        if(request.pointsPerTie()!=null)c.setPointsPerTie(request.pointsPerTie());
        if(request.pointsPerNoResult()!=null)c.setPointsPerNoResult(request.pointsPerNoResult());
        c.setQualifyingTeamCount(request.qualifyingTeamCount()); c.setVenues(join(request.venues(),"\n"));
        c.setLeagueStartAt(request.leagueStartAt()); c.setFixtureSpacingDays(request.fixtureSpacingDays());
        c.setManualDateEditing(request.manualDateEditing());
        if(request.allowedWeekdays()!=null)c.setAllowedWeekdays(join(request.allowedWeekdays(),","));
        if(request.dailyStartTime()!=null)c.setDailyStartTime(request.dailyStartTime());if(request.dailyEndTime()!=null)c.setDailyEndTime(request.dailyEndTime());
        if(request.fixtureSlotMinutes()!=null)c.setFixtureSlotMinutes(request.fixtureSlotMinutes());if(request.maximumFixturesPerDay()!=null)c.setMaximumFixturesPerDay(request.maximumFixturesPerDay());
        if(request.blackoutDates()!=null)c.setBlackoutDates(join(request.blackoutDates(),"\n"));if(request.minimumRestHours()!=null)c.setMinimumRestHours(request.minimumRestHours());
        if(request.simultaneousFixturesAllowed()!=null)c.setSimultaneousFixturesAllowed(request.simultaneousFixturesAllowed());if(request.teamMayPlayTwicePerDay()!=null)c.setTeamMayPlayTwicePerDay(request.teamMayPlayTwicePerDay());
        applyCommonPreset(c);
        List<String> errors=validateCommon(c,existingTeams,existingLargestRoster);
        if(!errors.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,String.join("; ",errors));
        engine(championship).validateConfiguration(c,existingTeams);
        c.setConfigurationStatus(ConfigurationStatus.DRAFT);
        c.setConfigurationVersion(c.getConfigurationVersion()+1);
        configs.save(c);
        AuctionConfiguration auctionPolicy=auctionConfigurations.findLatestForUpdate(cid).orElseThrow(()->notFound("Auction configuration"));
        if(!Objects.equals(before.minimumRosterSize(),c.getMinimumRosterSize())||!Objects.equals(before.maximumRosterSize(),c.getMaximumRosterSize())) {
            if(auctions.findAllByChampionshipIdOrderByCreatedAtDesc(cid).stream().anyMatch(a->a.getStatus()!=AuctionStatus.NOT_STARTED)) conflict("Roster rules are locked after the first auction action");
            auctionPolicy.setMinimumRosterSize(c.getMinimumRosterSize());auctionPolicy.setMaximumRosterSize(c.getMaximumRosterSize());auctionPolicy.setConfigurationVersion(auctionPolicy.getConfigurationVersion()+1);auctionConfigurations.save(auctionPolicy);
            TournamentSettings operational=settings.findByChampionshipId(cid).orElseThrow(()->notFound("Tournament settings"));
            if(operational.getMinMale()+operational.getMinFemale()>c.getMaximumRosterSize()) conflict("Gender minimums exceed the proposed roster size; update roster quotas first");
            operational.setMaxSquadSize(c.getMaximumRosterSize());settings.save(operational);
        }
        CompetitionConfigurationDto after=CompetitionDtos.from(c);
        audit(championship,null,actor,"COMPETITION_CONFIGURED",c.getConfigurationVersion(),Map.of("before",before,"after",after));
        if(!Objects.equals(before.presetCode(),after.presetCode()))audit(championship,null,actor,"CONFIGURATION_PRESET_APPLIED",c.getConfigurationVersion(),Map.of("before",Optional.ofNullable(before.presetCode()).orElse(""),"after",Optional.ofNullable(after.presetCode()).orElse("")));
        return after;
    }

    @Transactional public CompetitionOverviewDto generateLeague(UUID cid,User actor) {
        squads.assertLeagueReady(cid);
        Championship championship=lockedChampionship(cid);
        CompetitionConfiguration c=lockedConfig(cid);
        SportCompetitionEngine engine=engine(championship);
        List<Team> teamList=teams.findAllByChampionshipIdOrderByName(cid);
        if(teamList.size()<c.getMinimumTeamCount()||teamList.size()!=c.getConfiguredTeamCount())
            conflict("Provision exactly "+c.getConfiguredTeamCount()+" teams (minimum launch count "+c.getMinimumTeamCount()+") before generating fixtures");
        engine.validateConfiguration(c,teamList.size());
        if(c.getLeagueStartAt()==null) conflict("Configure the league start date before generating fixtures");
        int expected=expected(teamList.size(),c);
        List<Match> existing=fixtures.findAllByChampionshipIdAndStageTypeOrderByMatchNumberAsc(cid,CompetitionStageType.LEAGUE);
        if(!existing.isEmpty()) {
            if(existing.size()==expected&&existing.stream().allMatch(match->match.getFixtureKey()!=null))
                return overview(championship,c,fixtures.findAllByChampionshipIdOrderByMatchNumberAsc(cid));
            conflict("Existing league fixtures do not match the configured round robin");
        }
        CompetitionStage league=stage(championship,CompetitionStageType.LEAGUE,1);
        league.setStatus(CompetitionStageStatus.ACTIVE); league.setStartedAt(Instant.now()); stages.save(league);
        List<String> venues=venues(c);
        List<int[]> schedule=roundRobinIndexes(teamList.size(),c);
        List<Instant> scheduledTimes=scheduleTimes(schedule,c,venues.size());
        int number=fixtures.maxMatchNumber(cid);
        for(int index=0;index<schedule.size();index++) {
            int[] pair=schedule.get(index); Team a=teamList.get(pair[2]),b=teamList.get(pair[3]);
            Match fixture=fixtures.save(Match.builder().championship(championship).competitionStage(league).stageChampionshipId(cid)
                    .stageType(CompetitionStageType.LEAGUE).roundNumber(pair[0]).legNumber(pair[1])
                    .fixtureKey(fixtureKey(pair[0],pair[1],a,b)).teamA(a).teamB(b)
                    .scheduledAt(scheduledTimes.get(index))
                    .status(MatchStatus.SCHEDULED).teamAFormatWins(0).teamBFormatWins(0)
                    .teamAPoints(0).teamBPoints(0).resultRevision(0).matchNumber(++number)
                    .venue(venues.get(index%venues.size())).scoreData("{}").build());
            components(fixture,engine,c);
        }
        championship.setLifecyclePhase(ChampionshipPhase.LEAGUE_STAGE); championships.save(championship);
        c.setConfigurationStatus(ConfigurationStatus.LOCKED);c.setLockedAt(Instant.now());configs.save(c);
        configurationSections.lock(cid,"COMPETITION_FORMAT",actor);configurationSections.lock(cid,"SCHEDULING",actor);configurationSections.lock(cid,"QUALIFICATION_AND_PLAYOFFS",actor);
        audit(championship,null,actor,"LEAGUE_FIXTURES_GENERATED",1,
                Map.of("fixtureCount",schedule.size(),"roundRobinMode",c.getRoundRobinMode()));
        return overview(championship,c,fixtures.findAllByChampionshipIdOrderByMatchNumberAsc(cid));
    }

    @Transactional public MatchDto reschedule(UUID cid,UUID fixtureId,RescheduleFixtureRequest request,User actor) {
        CompetitionConfiguration scheduleConfig=lockedConfig(cid);if(!Boolean.TRUE.equals(scheduleConfig.getManualDateEditing())) conflict("Manual fixture date editing is disabled");
        Match fixture=lockedFixture(cid,fixtureId);
        if(fixture.getStatus()==MatchStatus.COMPLETED) conflict("Completed fixtures cannot be rescheduled");
        List<Match> other=fixtures.findAllByChampionshipIdOrderByMatchNumberAsc(cid).stream().filter(x->!x.getId().equals(fixtureId)).toList();List<String> schedulingErrors=validateScheduledAt(request.scheduledAt(),fixture.getTeamA().getId(),fixture.getTeamB().getId(),scheduleConfig,other);if(!schedulingErrors.isEmpty())conflict(String.join("; ",schedulingErrors));
        Map<String,Object> before=Map.of("scheduledAt",fixture.getScheduledAt(),
                "venue",Optional.ofNullable(fixture.getVenue()).orElse(""));
        fixture.setScheduledAt(request.scheduledAt()); fixture.setVenue(request.venue()==null?null:request.venue().trim());
        fixtures.save(fixture);
        audit(fixture.getChampionship(),fixture,actor,"FIXTURE_RESCHEDULED",fixture.getResultRevision()+1,
                Map.of("before",before,"scheduledAt",request.scheduledAt(),
                        "venue",Optional.ofNullable(fixture.getVenue()).orElse("")));
        return MatchDto.from(fixture);
    }

    @Transactional public List<StandingDto> standings(UUID cid,boolean penalties) {
        Championship championship=championship(cid); CompetitionConfiguration c=config(cid);
        List<Team> teamList=teams.findAllByChampionshipIdOrderByName(cid);
        List<Match> league=fixtures.findAllByChampionshipIdAndStageTypeOrderByMatchNumberAsc(cid,CompetitionStageType.LEAGUE);
        TournamentSettings legacy=settings.findByChampionshipId(cid).orElseThrow(()->notFound("Tournament settings"));
        Map<UUID,Set<UUID>> used=new HashMap<>();
        league.stream().filter(match->match.getStatus()==MatchStatus.COMPLETED).forEach(match->
                match.getFormats().forEach(format->{
                    format.getSideAPlayers().forEach(player->used.computeIfAbsent(match.getTeamA().getId(),key->new HashSet<>()).add(player.getId()));
                    format.getSideBPlayers().forEach(player->used.computeIfAbsent(match.getTeamB().getId(),key->new HashSet<>()).add(player.getId()));
                }));
        List<Player> roster=players.findAllByChampionshipIdAndTeamIsNotNull(cid);
        Map<UUID,List<UUID>> unplayed=new HashMap<>();
        teamList.forEach(team->unplayed.put(team.getId(),roster.stream()
                .filter(player->player.getTeam()!=null&&player.getTeam().getId().equals(team.getId()))
                .map(Player::getId).filter(id->!used.getOrDefault(team.getId(),Set.of()).contains(id)).toList()));
        return engine(championship).calculateStandings(c,legacy,teamList,league,unplayed,penalties);
    }

    @Transactional public CompetitionOverviewDto generatePlayoffs(UUID cid,User actor) {
        Championship championship=lockedChampionship(cid); CompetitionConfiguration c=lockedConfig(cid);
        List<Team> teamList=teams.findAllByChampionshipIdOrderByName(cid);
        List<Match> league=fixtures.findAllByChampionshipIdAndStageTypeOrderByMatchNumberAsc(cid,CompetitionStageType.LEAGUE);
        if(league.size()!=expected(teamList.size(),c)||
                league.stream().anyMatch(match->match.getStatus()!=MatchStatus.COMPLETED))
            conflict("Every generated league fixture must be completed before playoff generation");
        if(fixtures.existsByChampionshipIdAndStageType(cid,CompetitionStageType.SEMI_FINAL)||
                fixtures.existsByChampionshipIdAndStageType(cid,CompetitionStageType.FINAL))
            return overview(championship,c,fixtures.findAllByChampionshipIdOrderByMatchNumberAsc(cid));
        List<StandingDto> table=standings(cid,true);
        if(table.size()<c.getQualifyingTeamCount()) conflict("Not enough ranked teams are available for playoffs");
        CompetitionStage leagueStage=stages.findForUpdate(cid,CompetitionStageType.LEAGUE).orElseThrow(()->notFound("League stage"));
        leagueStage.setStatus(CompetitionStageStatus.FROZEN); leagueStage.setCompletedAt(Instant.now());
        leagueStage.setFrozenAt(Instant.now()); leagueStage.setStandingsSnapshot(toJson(table)); stages.save(leagueStage);
        if(!qualifications.existsByChampionshipIdAndSourceStage(cid,CompetitionStageType.LEAGUE)) {
            Map<UUID,Team> byId=teamList.stream().collect(Collectors.toMap(Team::getId,Function.identity()));
            table.stream().limit(c.getQualifyingTeamCount()).forEach(row->qualifications.save(
                    CompetitionQualification.builder().championship(championship).sourceStage(CompetitionStageType.LEAGUE)
                            .team(byId.get(row.team().id())).seed(row.rank()).build()));
        }
        Map<Integer,Team> seeded=qualifications.findAllByChampionshipIdAndSourceStageOrderBySeed(
                cid,CompetitionStageType.LEAGUE).stream().collect(Collectors.toMap(
                        CompetitionQualification::getSeed,CompetitionQualification::getTeam));
        Instant start=league.stream().map(Match::getScheduledAt).max(Comparator.naturalOrder()).orElse(Instant.now())
                .plus(c.getFixtureSpacingDays(),ChronoUnit.DAYS);
        List<String> venues=venues(c); int number=fixtures.maxMatchNumber(cid),index=0;
        if(c.getPlayoffFormat()==PlayoffFormat.NONE) conflict("This championship is configured without playoffs");
        if(c.getPlayoffFormat()==PlayoffFormat.TOP_TWO_FINAL){
            CompetitionStage finalStage=stage(championship,CompetitionStageType.FINAL,2);
            finalStage.setStatus(CompetitionStageStatus.ACTIVE);finalStage.setStartedAt(Instant.now());stages.save(finalStage);
            Match finalFixture=fixtures.save(Match.builder().championship(championship).competitionStage(finalStage)
                    .stageChampionshipId(cid).stageType(CompetitionStageType.FINAL).roundNumber(1).legNumber(1)
                    .fixtureKey("FINAL-S1-S2").seedA(1).seedB(2).teamA(seeded.get(1)).teamB(seeded.get(2))
                    .scheduledAt(start).status(MatchStatus.SCHEDULED).teamAFormatWins(0).teamBFormatWins(0)
                    .teamAPoints(0).teamBPoints(0).resultRevision(0).matchNumber(++number).venue(venues.get(0)).scoreData("{}").build());
            components(finalFixture,engine(championship),c);championship.setLifecyclePhase(ChampionshipPhase.FINAL);championships.save(championship);
            audit(championship,finalFixture,actor,"FINAL_GENERATED",1,Map.of("seedA",1,"seedB",2));
            return overview(championship,c,fixtures.findAllByChampionshipIdOrderByMatchNumberAsc(cid));
        }
        CompetitionStage semifinal=stage(championship,CompetitionStageType.SEMI_FINAL,2);
        semifinal.setStatus(CompetitionStageStatus.ACTIVE); semifinal.setStartedAt(Instant.now()); stages.save(semifinal);
        for(var seeds:engine(championship).playoffPairings(c.getQualifyingTeamCount())) {
            Team a=seeded.get(seeds.seedA()),b=seeded.get(seeds.seedB());
            if(a==null||b==null||a.getId().equals(b.getId())) conflict("Invalid playoff seeds");
            Match fixture=fixtures.save(Match.builder().championship(championship).competitionStage(semifinal).stageChampionshipId(cid)
                    .stageType(CompetitionStageType.SEMI_FINAL).roundNumber(1).legNumber(1)
                    .fixtureKey("SEMI_FINAL-S"+seeds.seedA()+"-S"+seeds.seedB()).seedA(seeds.seedA()).seedB(seeds.seedB())
                    .teamA(a).teamB(b).scheduledAt(start.plus((long)index*c.getFixtureSpacingDays(),ChronoUnit.DAYS))
                    .status(MatchStatus.SCHEDULED).teamAFormatWins(0).teamBFormatWins(0).teamAPoints(0).teamBPoints(0)
                    .resultRevision(0).matchNumber(++number).venue(venues.get(index++%venues.size())).scoreData("{}").build());
            components(fixture,engine(championship),c);
        }
        championship.setLifecyclePhase(ChampionshipPhase.PLAYOFFS); championships.save(championship);
        audit(championship,null,actor,"PLAYOFFS_GENERATED",1,Map.of("standings",table,"qualifiers",c.getQualifyingTeamCount()));
        return overview(championship,c,fixtures.findAllByChampionshipIdOrderByMatchNumberAsc(cid));
    }

    @Transactional public void advanceAfterCompletion(UUID cid,UUID fixtureId,User actor) {
        Championship championship=lockedChampionship(cid); Match completed=lockedFixture(cid,fixtureId);
        if(completed.getStatus()!=MatchStatus.COMPLETED) return;
        if(completed.getStageType()==CompetitionStageType.LEAGUE) {
            List<Match> league=fixtures.findAllByChampionshipIdAndStageTypeOrderByMatchNumberAsc(cid,CompetitionStageType.LEAGUE);
            if(!league.isEmpty()&&league.stream().allMatch(match->match.getStatus()==MatchStatus.COMPLETED))
                stages.findForUpdate(cid,CompetitionStageType.LEAGUE).ifPresent(stage->{
                    stage.setStatus(CompetitionStageStatus.COMPLETED); stage.setCompletedAt(Instant.now()); stages.save(stage);
                });
        } else if(completed.getStageType()==CompetitionStageType.SEMI_FINAL&&completed.getWinnerTeam()!=null) {
            List<Match> semis=fixtures.findAllByChampionshipIdAndStageTypeOrderByMatchNumberAsc(cid,CompetitionStageType.SEMI_FINAL);
            if(!semis.isEmpty()&&semis.stream().allMatch(match->match.getStatus()==MatchStatus.COMPLETED))
                createFinal(championship,semis,actor);
        } else if(completed.getStageType()==CompetitionStageType.FINAL&&completed.getWinnerTeam()!=null) {
            CompetitionConfiguration c=lockedConfig(cid); Team champion=completed.getWinnerTeam();
            Team runner=champion.getId().equals(completed.getTeamA().getId())?completed.getTeamB():completed.getTeamA();
            c.setChampion(champion); c.setRunnerUp(runner); configs.save(c);
            stages.findForUpdate(cid,CompetitionStageType.FINAL).ifPresent(stage->{
                stage.setStatus(CompetitionStageStatus.COMPLETED); stage.setCompletedAt(Instant.now()); stages.save(stage);
            });
            championship.setLifecyclePhase(ChampionshipPhase.COMPLETED);
            championship.setStatus(ChampionshipStatus.COMPLETED); championships.save(championship);
            audit(championship,completed,actor,"CHAMPIONSHIP_COMPLETED",completed.getResultRevision(),
                    Map.of("championTeamId",champion.getId(),"runnerUpTeamId",runner.getId()));
        }
    }

    private void createFinal(Championship championship,List<Match> semis,User actor) {
        UUID cid=championship.getId();
        if(fixtures.existsByChampionshipIdAndStageType(cid,CompetitionStageType.FINAL)) return;
        List<Team> winners=semis.stream().sorted(Comparator.comparing(Match::getMatchNumber)).map(Match::getWinnerTeam).toList();
        if(winners.size()!=2||winners.get(0).getId().equals(winners.get(1).getId())) conflict("Invalid semifinal winners");
        CompetitionConfiguration c=config(cid); CompetitionStage finalStage=stage(championship,CompetitionStageType.FINAL,3);
        finalStage.setStatus(CompetitionStageStatus.ACTIVE); finalStage.setStartedAt(Instant.now()); stages.save(finalStage);
        stages.findForUpdate(cid,CompetitionStageType.SEMI_FINAL).ifPresent(stage->{
            stage.setStatus(CompetitionStageStatus.COMPLETED); stage.setCompletedAt(Instant.now()); stages.save(stage);
        });
        Instant at=semis.stream().map(Match::getScheduledAt).max(Comparator.naturalOrder()).orElse(Instant.now())
                .plus(c.getFixtureSpacingDays(),ChronoUnit.DAYS);
        Match finalFixture=fixtures.save(Match.builder().championship(championship).competitionStage(finalStage).stageChampionshipId(cid)
                .stageType(CompetitionStageType.FINAL).roundNumber(1).legNumber(1).fixtureKey("FINAL")
                .teamA(winners.get(0)).teamB(winners.get(1)).scheduledAt(at).status(MatchStatus.SCHEDULED)
                .teamAFormatWins(0).teamBFormatWins(0).teamAPoints(0).teamBPoints(0).resultRevision(0)
                .matchNumber(fixtures.maxMatchNumber(cid)+1).venue(venues(c).get(0))
                .scoreData("{}").build());
        components(finalFixture,engine(championship),c);
        if(c.getPlayoffFormat()==PlayoffFormat.SEMI_FINALS_THIRD_PLACE_AND_FINAL){
            List<Team> losers=semis.stream().sorted(Comparator.comparing(Match::getMatchNumber)).map(m->m.getWinnerTeam().getId().equals(m.getTeamA().getId())?m.getTeamB():m.getTeamA()).toList();
            CompetitionStage thirdStage=stage(championship,CompetitionStageType.THIRD_PLACE,3);
            thirdStage.setStatus(CompetitionStageStatus.ACTIVE);thirdStage.setStartedAt(Instant.now());stages.save(thirdStage);
            Match third=fixtures.save(Match.builder().championship(championship).competitionStage(thirdStage).stageChampionshipId(cid)
                    .stageType(CompetitionStageType.THIRD_PLACE).roundNumber(1).legNumber(1).fixtureKey("THIRD_PLACE")
                    .teamA(losers.get(0)).teamB(losers.get(1)).scheduledAt(at).status(MatchStatus.SCHEDULED)
                    .teamAFormatWins(0).teamBFormatWins(0).teamAPoints(0).teamBPoints(0).resultRevision(0)
                    .matchNumber(fixtures.maxMatchNumber(cid)+1).venue(venues(c).get(0)).scoreData("{}").build());
            components(third,engine(championship),c);
        }
        championship.setLifecyclePhase(ChampionshipPhase.FINAL); championships.save(championship);
        audit(championship,finalFixture,actor,"FINAL_GENERATED",1,
                Map.of("teamAId",winners.get(0).getId(),"teamBId",winners.get(1).getId()));
    }

    @Transactional public DashboardCapabilitiesDto dashboard(UUID cid,Authentication auth) {
        Championship championship=championship(cid); CompetitionConfiguration c=config(cid);
        SportCompetitionEngine engine=engines.supports(championship.getSportType())?engine(championship):null;
        List<Match> all=fixtures.findAllByChampionshipIdOrderByMatchNumberAsc(cid);
        List<StandingDto> table=engine==null?List.of():standings(cid,true); var squad=squads.summary(cid);
        boolean admin=isAdmin(cid,auth);
        boolean configured=engine!=null&&c.getLeagueStartAt()!=null&&!venues(c).isEmpty();
        boolean leagueExists=all.stream().anyMatch(match->match.getStageType()==CompetitionStageType.LEAGUE);
        boolean leagueComplete=leagueExists&&all.stream().filter(match->match.getStageType()==CompetitionStageType.LEAGUE)
                .allMatch(match->match.getStatus()==MatchStatus.COMPLETED);
        List<String> blockers=new ArrayList<>(),actions=new ArrayList<>();
        if(!configured) blockers.add("Complete league configuration");
        if(engine==null) blockers.add("Install a competition engine for "+SportEngineRegistry.normalize(championship.getSportType()));
        if(!squad.allSquadsLocked()) blockers.add("Lock every squad");
        if(leagueExists&&!leagueComplete) blockers.add("Complete every league fixture");
        if(admin&&!leagueExists) actions.add("CONFIGURE_COMPETITION");
        if(admin&&configured&&squad.allSquadsLocked()&&!leagueExists) actions.add("GENERATE_LEAGUE");
        if(admin&&leagueExists) actions.add("MANAGE_RESULTS");
        if(admin&&leagueComplete&&!fixtures.existsByChampionshipIdAndStageType(cid,CompetitionStageType.SEMI_FINAL))
            actions.add("GENERATE_PLAYOFFS");
        if(admin&&Boolean.TRUE.equals(c.getManualDateEditing())) actions.add("RESCHEDULE_FIXTURES");
        List<MatchDto> upcoming=all.stream().filter(match->match.getStatus()!=MatchStatus.COMPLETED)
                .sorted(Comparator.comparing(Match::getScheduledAt)).limit(5).map(MatchDto::from).toList();
        List<MatchDto> recent=all.stream().filter(match->match.getStatus()==MatchStatus.COMPLETED)
                .sorted(Comparator.comparing(Match::getScheduledAt).reversed()).limit(5).map(MatchDto::from).toList();
        String sportCode=SportEngineRegistry.normalize(championship.getSportType());
        return new DashboardCapabilitiesDto(sportCode,championship.getLifecyclePhase(),actions,
                engine==null?List.of("UPCOMING_FIXTURES"):engine.supportedWidgets(),
                engine==null?Map.of("fixture","Match","standings","Standings"):engine.terminology(),
                engine==null?Map.of():engine.dashboardMetrics(table,all),configured,blockers,
                teams.findAllByChampionshipIdOrderByName(cid).size(),
                (int)players.countByChampionshipId(cid),
                squad.lockedTeams(),squad.totalTeams(),upcoming,recent,table,
                TeamRef.from(c.getChampion()),TeamRef.from(c.getRunnerUp()));
    }

    @Transactional public List<AuditEventDto> auditHistory(UUID cid) {
        championship(cid); return audits.findTop100ByChampionshipIdOrderByOccurredAtDesc(cid).stream()
                .map(AuditEventDto::from).toList();
    }
    @Transactional public ConfigurationWorkspaceDto workspace(UUID cid,Authentication auth){
        Championship championship=championship(cid);CompetitionConfiguration c=config(cid);List<Match> all=fixtures.findAllByChampionshipIdOrderByMatchNumberAsc(cid);
        boolean hasTeams=!teams.findAllByChampionshipIdOrderByName(cid).isEmpty(),hasFixtures=!all.isEmpty(),hasScores=all.stream().anyMatch(m->m.getStatus()!=MatchStatus.SCHEDULED);
        Map<String,ConfigurationSectionState> persisted=configurationSections.all(cid).stream().collect(Collectors.toMap(ConfigurationSectionState::getSectionCode,Function.identity()));
        List<ConfigurationSectionDto> sections=List.of(
                section("BASICS",persisted.get("BASICS"),sectionStatus(persisted,"BASICS",c),!hasTeams&&!locked(persisted,"BASICS"),List.of(),List.of(),List.of(field("configuredTeamCount","INTEGER",2,null,List.of(),"Locks after team provisioning"),field("timezone","TIMEZONE",null,null,List.of(),null))),
                section("BRANDING",persisted.get("BRANDING"),sectionStatus(persisted,"BRANDING",c),!locked(persisted,"BRANDING"),List.of("BASICS"),List.of(),List.of(field("displayName","STRING",1,150,List.of(),null),field("publicVisibility","BOOLEAN",null,null,List.of(),null))),
                section("TEAMS_AND_ROSTERS",persisted.get("TEAMS_AND_ROSTERS"),sectionStatus(persisted,"TEAMS_AND_ROSTERS",c),!squads.summary(cid).allSquadsLocked()&&!locked(persisted,"TEAMS_AND_ROSTERS"),List.of("BASICS"),List.of(),List.of(field("minimumRosterSize","INTEGER",1,null,List.of(),null),field("maximumRosterSize","INTEGER",1,null,List.of(),"Cannot be below an existing roster"),field("playingSideSize","INTEGER",1,c.getMaximumRosterSize(),List.of(),null))),
                section("AUCTION",persisted.get("AUCTION"),sectionStatus(persisted,"AUCTION",c),!locked(persisted,"AUCTION"),List.of("TEAMS_AND_ROSTERS"),List.of(),List.of(field("auctionType","ENUM",null,null,List.of("OPEN_ASCENDING"),"Only the enforced open-ascending workflow is currently available; locks after first auction action"),field("teamBudget","DECIMAL",0,null,List.of(),null))),
                section("SPORT_RULES",persisted.get("SPORT_RULES"),sectionStatus(persisted,"SPORT_RULES",c),!hasScores&&!locked(persisted,"SPORT_RULES"),List.of("TEAMS_AND_ROSTERS"),List.of(),List.of(field("presetCode","PRESET",null,null,championship.getSportType().equalsIgnoreCase("CRICKET")?List.of("T10","T20","ODI","CUSTOM"):List.of("BADMINTON_STANDARD_FIVE","BADMINTON_COMPACT_THREE","CUSTOM"),"Locks after scoring starts"))),
                section("COMPETITION_FORMAT",persisted.get("COMPETITION_FORMAT"),sectionStatus(persisted,"COMPETITION_FORMAT",c),!hasFixtures&&!locked(persisted,"COMPETITION_FORMAT"),List.of("SPORT_RULES"),List.of(),List.of(field("roundRobinMode","ENUM",null,null,List.of("SINGLE","DOUBLE","CUSTOM"),"Locks after fixture generation"),field("playoffFormat","ENUM",null,null,Arrays.stream(PlayoffFormat.values()).map(Enum::name).toList(),null))),
                section("SCHEDULING",persisted.get("SCHEDULING"),sectionStatus(persisted,"SCHEDULING",c),!hasFixtures&&!locked(persisted,"SCHEDULING"),List.of("COMPETITION_FORMAT"),List.of(),List.of(field("venues","STRING_LIST",1,null,List.of(),null),field("allowedWeekdays","ENUM_LIST",1,7,Arrays.stream(DayOfWeek.values()).map(Enum::name).toList(),null),field("maximumFixturesPerDay","INTEGER",1,null,List.of(),null))),
                section("QUALIFICATION_AND_PLAYOFFS",persisted.get("QUALIFICATION_AND_PLAYOFFS"),sectionStatus(persisted,"QUALIFICATION_AND_PLAYOFFS",c),!hasFixtures&&!locked(persisted,"QUALIFICATION_AND_PLAYOFFS"),List.of("COMPETITION_FORMAT"),List.of(),List.of(field("qualifyingTeamCount","INTEGER",0,c.getConfiguredTeamCount(),List.of(),null))),
                section("PERMISSIONS_AND_VISIBILITY",persisted.get("PERMISSIONS_AND_VISIBILITY"),sectionStatus(persisted,"PERMISSIONS_AND_VISIBILITY",c),!locked(persisted,"PERMISSIONS_AND_VISIBILITY"),List.of("BRANDING"),List.of(),List.of(field("publicVisibility","BOOLEAN",null,null,List.of(),null))),
                section("REVIEW_AND_LAUNCH",persisted.get("REVIEW_AND_LAUNCH"),sectionStatus(persisted,"REVIEW_AND_LAUNCH",c),!hasFixtures&&!locked(persisted,"REVIEW_AND_LAUNCH"),List.of("BASICS","BRANDING","TEAMS_AND_ROSTERS","AUCTION","SPORT_RULES","COMPETITION_FORMAT","SCHEDULING"),List.of(),List.of()));
        List<String> actions=new ArrayList<>();if(isAdmin(cid,auth)&&!hasFixtures)actions.addAll(List.of("SAVE_DRAFT","VALIDATE_CONFIGURATION"));if(isAdmin(cid,auth)&&c.getConfigurationStatus()!=ConfigurationStatus.DRAFT&&!hasFixtures)actions.add("REOPEN_CONFIGURATION");
        List<String> presets=new ArrayList<>(List.of("SMALL_CORPORATE_LEAGUE","STANDARD_CORPORATE_LEAGUE","CUSTOM_CHAMPIONSHIP"));presets.addAll(championship.getSportType().equalsIgnoreCase("CRICKET")?List.of("T10","T20","ODI","CUSTOM"):List.of("BADMINTON_STANDARD_FIVE","BADMINTON_COMPACT_THREE","CUSTOM"));return new ConfigurationWorkspaceDto(c.getConfigurationVersion(),presets,sections,actions,phaseReadiness(cid,next(championship.getLifecyclePhase())).blockers());
    }
    @Transactional public ConfigurationWorkspaceDto validateConfiguration(UUID cid,User actor,Authentication auth){
        Championship ch=lockedChampionship(cid);CompetitionConfiguration c=lockedConfig(cid);int teamCount=teams.findAllByChampionshipIdOrderByName(cid).size();int largest=players.findAllByChampionshipIdOrderByAuctionOrderAsc(cid).stream().filter(p->p.getTeam()!=null).collect(Collectors.groupingBy(p->p.getTeam().getId(),Collectors.counting())).values().stream().mapToInt(Long::intValue).max().orElse(0);
        List<String> errors=validateCommon(c,teamCount,largest);Map<String,ConfigurationStatus> sectionStates=configurationSections.all(cid).stream().collect(Collectors.toMap(ConfigurationSectionState::getSectionCode,ConfigurationSectionState::getStatus));for(String required:List.of("BRANDING","AUCTION","SPORT_RULES"))if(sectionStates.getOrDefault(required,ConfigurationStatus.DRAFT)==ConfigurationStatus.DRAFT)errors.add(required.replace('_',' ')+" section must be saved and valid");
        try{engine(ch).validateConfiguration(c,teamCount);}catch(ResponseStatusException e){errors.add(Optional.ofNullable(e.getReason()).orElse("Sport configuration is invalid"));}if(!errors.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,String.join("; ",errors));c.setConfigurationStatus(ConfigurationStatus.VALID);configs.save(c);for(String section:List.of("BASICS","TEAMS_AND_ROSTERS","COMPETITION_FORMAT","SCHEDULING","QUALIFICATION_AND_PLAYOFFS","PERMISSIONS_AND_VISIBILITY","REVIEW_AND_LAUNCH"))configurationSections.markValid(cid,section,actor,List.of());audit(ch,null,actor,"CONFIGURATION_VALIDATED",c.getConfigurationVersion(),Map.of("status","VALID"));return workspace(cid,auth);
    }
    @Transactional public ConfigurationWorkspaceDto reopenConfiguration(UUID cid,ConfigurationActionRequest request,User actor,Authentication auth){Championship ch=lockedChampionship(cid);CompetitionConfiguration c=lockedConfig(cid);if(!Objects.equals(request.expectedVersion(),c.getConfigurationVersion()))conflict("Configuration version conflict: reload before reopening");if(!fixtures.findAllByChampionshipIdOrderByMatchNumberAsc(cid).isEmpty())conflict("Configuration cannot be reopened after fixtures exist; downstream state cannot be rebuilt safely");ConfigurationStatus before=c.getConfigurationStatus();c.setConfigurationStatus(ConfigurationStatus.DRAFT);c.setConfigurationVersion(c.getConfigurationVersion()+1);c.setLockedAt(null);configs.save(c);configurationSections.reopenEditable(cid,actor);audit(ch,null,actor,"CONFIGURATION_REOPENED",c.getConfigurationVersion(),Map.of("before",before,"after",c.getConfigurationStatus(),"reason",request.reason()));return workspace(cid,auth);}
    @Transactional public PhaseReadinessDto phaseReadiness(UUID cid,ChampionshipPhase target){Championship ch=championship(cid);CompetitionConfiguration c=config(cid);List<String> blockers=new ArrayList<>();if(target.ordinal()!=ch.getLifecyclePhase().ordinal()+1&&target!=ch.getLifecyclePhase())blockers.add("Target phase must immediately follow the current phase");int teamCount=teams.findAllByChampionshipIdOrderByName(cid).size();boolean auctionEnabled=auctionConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(cid).map(AuctionConfiguration::getEnabled).orElse(true);switch(target){case TEAM_PROVISIONING->{if(c.getConfigurationStatus()!=ConfigurationStatus.VALID)blockers.add("Validate championship configuration");}case PLAYER_REGISTRATION->{if(teamCount<c.getMinimumTeamCount()||teamCount!=c.getConfiguredTeamCount())blockers.add("Provision exactly "+c.getConfiguredTeamCount()+" teams");}case AUCTION->{if(players.findAllByChampionshipIdOrderByAuctionOrderAsc(cid).isEmpty())blockers.add("Register eligible players");}case SQUAD_CONFIRMATION->{if(auctionEnabled&&!auctions.existsByChampionshipIdAndStatus(cid,AuctionStatus.COMPLETED))blockers.add("Complete the auction");}case LEAGUE_STAGE->{if(!squads.summary(cid).allSquadsLocked())blockers.add("Lock every valid squad");if(c.getLeagueStartAt()==null)blockers.add("Configure league scheduling");}case PLAYOFFS->{List<Match> league=fixtures.findAllByChampionshipIdAndStageTypeOrderByMatchNumberAsc(cid,CompetitionStageType.LEAGUE);if(league.isEmpty()||league.stream().anyMatch(m->m.getStatus()!=MatchStatus.COMPLETED))blockers.add("Complete every league fixture");}case FINAL->{if(!fixtures.existsByChampionshipIdAndStageType(cid,CompetitionStageType.FINAL))blockers.add("Generate the final fixture");}case COMPLETED->{List<Match> finals=fixtures.findAllByChampionshipIdAndStageTypeOrderByMatchNumberAsc(cid,CompetitionStageType.FINAL);if(finals.isEmpty()||finals.stream().anyMatch(m->m.getStatus()!=MatchStatus.COMPLETED))blockers.add("Confirm the final result");}default->{}}return new PhaseReadinessDto(ch.getLifecyclePhase(),target,blockers.isEmpty(),blockers);}
    @Transactional public PhaseReadinessDto advancePhase(UUID cid,ChampionshipPhase target,User actor){Championship ch=lockedChampionship(cid);PhaseReadinessDto ready=phaseReadiness(cid,target);if(!ready.ready())conflict(String.join("; ",ready.blockers()));ChampionshipPhase before=ch.getLifecyclePhase();ch.setLifecyclePhase(target);championships.save(ch);audit(ch,null,actor,"PHASE_PROGRESSED",config(cid).getConfigurationVersion(),Map.of("before",before,"after",target));return new PhaseReadinessDto(target,target,true,List.of());}
    public boolean hasDownstreamFixture(UUID cid,CompetitionStageType type) {
        return type==CompetitionStageType.LEAGUE
                ?fixtures.existsByChampionshipIdAndStageType(cid,CompetitionStageType.SEMI_FINAL)
                :type==CompetitionStageType.SEMI_FINAL&&fixtures.existsByChampionshipIdAndStageType(cid,CompetitionStageType.FINAL);
    }
    public void recordResultAudit(Match fixture,User actor,String type,Object payload) {
        audit(fixture.getChampionship(),fixture,actor,type,fixture.getResultRevision(),payload);
    }
    public void recordConfigurationAudit(UUID cid,User actor,String type,int version,Object payload){audit(championship(cid),null,actor,type,version,payload);}

    private CompetitionOverviewDto overview(Championship championship,CompetitionConfiguration c,List<Match> all) {
        List<CompetitionQualification> seeded=qualifications.findAllByChampionshipIdAndSourceStageOrderBySeed(
                championship.getId(),CompetitionStageType.LEAGUE);
        boolean complete=all.stream().anyMatch(match->match.getStageType()==CompetitionStageType.LEAGUE)&&
                all.stream().filter(match->match.getStageType()==CompetitionStageType.LEAGUE)
                        .allMatch(match->match.getStatus()==MatchStatus.COMPLETED);
        return new CompetitionOverviewDto(CompetitionDtos.from(c),
                stages.findAllByChampionshipIdOrderByStageOrder(championship.getId()).stream().map(StageDto::from).toList(),
                seeded.stream().map(QualificationDto::from).toList(),all.stream().map(MatchDto::from).toList(),complete,
                all.stream().anyMatch(match->match.getStageType()==CompetitionStageType.SEMI_FINAL),
                all.stream().anyMatch(match->match.getStageType()==CompetitionStageType.FINAL));
    }

    static List<int[]> roundRobinIndexes(int teamCount,RoundRobinMode mode) {
        List<Integer> rotating=new ArrayList<>(); for(int i=0;i<teamCount;i++) rotating.add(i);
        if(rotating.size()%2!=0) rotating.add(-1);
        List<int[]> first=new ArrayList<>(); int size=rotating.size();
        for(int round=1;round<size;round++) {
            for(int index=0;index<size/2;index++) {
                int left=rotating.get(index),right=rotating.get(size-1-index);
                if(left>=0&&right>=0) first.add(round%2==0?new int[]{round,1,right,left}:new int[]{round,1,left,right});
            }
            Integer last=rotating.remove(size-1); rotating.add(1,last);
        }
        if(mode==RoundRobinMode.SINGLE) return first;
        List<int[]> all=new ArrayList<>(first); first.forEach(pair->all.add(new int[]{pair[0],2,pair[3],pair[2]})); return all;
    }
    static List<int[]> roundRobinIndexes(int teamCount,CompetitionConfiguration config){
        if(config.getRoundRobinMode()!=RoundRobinMode.CUSTOM)return roundRobinIndexes(teamCount,config.getRoundRobinMode());
        List<int[]> first=roundRobinIndexes(teamCount,RoundRobinMode.SINGLE),all=new ArrayList<>();
        for(int leg=1;leg<=config.getNumberOfLegs();leg++)for(int[] pair:first)
            all.add(leg%2==1?new int[]{pair[0],leg,pair[2],pair[3]}:new int[]{pair[0],leg,pair[3],pair[2]});
        return all;
    }
    private int expected(int count,CompetitionConfiguration config){int single=count*(count-1)/2;return single*(config.getRoundRobinMode()==RoundRobinMode.SINGLE?1:config.getRoundRobinMode()==RoundRobinMode.DOUBLE?2:config.getNumberOfLegs());}
    private List<String> validateCommon(CompetitionConfiguration c,int existingTeams,int largestRoster){
        List<String> errors=new ArrayList<>();
        if(c.getMinimumTeamCount()<2)errors.add("Minimum team count must be at least two");
        if(c.getMinimumTeamCount()>c.getConfiguredTeamCount()||c.getConfiguredTeamCount()>c.getMaximumTeamCount())errors.add("Team counts must satisfy minimum <= configured <= maximum");
        if(c.getMaximumTeamCount()<existingTeams||c.getConfiguredTeamCount()<existingTeams)errors.add("Team limits cannot be reduced below the "+existingTeams+" existing teams");
        if(c.getMinimumRosterSize()<1||c.getMinimumRosterSize()>c.getMaximumRosterSize())errors.add("Roster limits must satisfy 1 <= minimum <= maximum");
        if(c.getMaximumRosterSize()<largestRoster)errors.add("Maximum roster size cannot be reduced below the existing roster size of "+largestRoster);
        if(c.getPlayingSideSize()<1||c.getPlayingSideSize()>c.getMaximumRosterSize())errors.add("Playing-side size must be between one and the maximum roster size");
        if(c.getMaximumSubstitutes()<0)errors.add("Maximum substitutes cannot be negative");
        if(c.getAllowedWeekdays()==null||c.getAllowedWeekdays().isBlank())errors.add("Select at least one allowed fixture weekday");
        if(!c.getDailyStartTime().isBefore(c.getDailyEndTime()))errors.add("Scheduling start time must be before end time");
        if(c.getFixtureSlotMinutes()<15)errors.add("Fixture slot duration must be at least 15 minutes");
        if(c.getMaximumFixturesPerDay()<1)errors.add("Maximum fixtures per day must be at least one");
        if(c.getMinimumRestHours()<0)errors.add("Minimum rest hours cannot be negative");
        try{allowedDays(c);}catch(Exception ignored){errors.add("Allowed weekdays contain an invalid value");}try{blackoutDates(c);}catch(Exception ignored){errors.add("Blackout dates must use ISO yyyy-MM-dd format");}
        if(c.getRoundRobinMode()==RoundRobinMode.CUSTOM&&c.getNumberOfLegs()<1)errors.add("Custom round robin requires at least one leg");
        int qualifiers=switch(c.getPlayoffFormat()){case NONE->0;case TOP_TWO_FINAL->2;default->4;};
        if(c.getQualifyingTeamCount()!=qualifiers)errors.add(c.getPlayoffFormat()+" requires "+qualifiers+" qualifiers");
        try{java.time.ZoneId.of(c.getTimezone());}catch(Exception ignored){errors.add("Timezone must be a valid IANA zone");}
        return errors;
    }
    private void applyCommonPreset(CompetitionConfiguration c){boolean cricket="CRICKET".equalsIgnoreCase(c.getSportCode());switch(Optional.ofNullable(c.getPresetCode()).orElse("CUSTOM_CHAMPIONSHIP")){case "SMALL_CORPORATE_LEAGUE"->{c.setConfiguredTeamCount(4);c.setMinimumTeamCount(4);c.setMaximumTeamCount(4);c.setMinimumRosterSize(cricket?12:8);c.setMaximumRosterSize(cricket?16:12);c.setQualifyingTeamCount(2);c.setPlayoffFormat(PlayoffFormat.TOP_TWO_FINAL);c.setRoundRobinMode(RoundRobinMode.SINGLE);c.setNumberOfLegs(1);}case "STANDARD_CORPORATE_LEAGUE"->{c.setConfiguredTeamCount(cricket?8:6);c.setMinimumTeamCount(c.getConfiguredTeamCount());c.setMaximumTeamCount(c.getConfiguredTeamCount());c.setMinimumRosterSize(cricket?14:8);c.setMaximumRosterSize(cricket?18:12);c.setQualifyingTeamCount(4);c.setPlayoffFormat(PlayoffFormat.SEMI_FINALS_AND_FINAL);c.setRoundRobinMode(RoundRobinMode.SINGLE);c.setNumberOfLegs(1);}default->{}}}
    private ConfigurationSectionDto section(String code,ConfigurationSectionState state,ConfigurationStatus status,boolean editable,List<String> dependencies,List<String> messages,List<ConfigurationFieldSchema> fields){return new ConfigurationSectionDto(code,status,editable,state==null?1:state.getConfigurationVersion(),state==null||state.getLastChangedBy()==null?null:state.getLastChangedBy().getFullName(),state==null?null:state.getUpdatedAt(),state==null?null:state.getLockedAt(),dependencies,messages,fields);}
    private ConfigurationFieldSchema field(String name,String type,Object min,Object max,List<String> values,String dependency){return new ConfigurationFieldSchema(name,type,min,max,values,dependency);}
    private ChampionshipPhase next(ChampionshipPhase phase){int n=phase.ordinal()+1;return n<ChampionshipPhase.values().length?ChampionshipPhase.values()[n]:phase;}
    private boolean locked(Map<String,ConfigurationSectionState> states,String code){return states.containsKey(code)&&states.get(code).getStatus()==ConfigurationStatus.LOCKED;}
    private ConfigurationStatus sectionStatus(Map<String,ConfigurationSectionState> states,String code,CompetitionConfiguration fallback){return states.containsKey(code)?states.get(code).getStatus():fallback.getConfigurationStatus();}
    private String fixtureKey(int round,int leg,Team a,Team b) {List<String> ids=new ArrayList<>(List.of(a.getId().toString(),b.getId().toString()));Collections.sort(ids);return "LEAGUE-L"+leg+"-R"+round+"-"+ids.get(0)+"-"+ids.get(1);}
    private List<String> venues(CompetitionConfiguration config) {return Arrays.stream(config.getVenues().split("\\R")).map(String::trim).filter(value->!value.isBlank()).toList();}
    static List<Instant> scheduleTimes(List<int[]> pairs,CompetitionConfiguration c,int venueCount){ZoneId zone=ZoneId.of(c.getTimezone());Set<DayOfWeek> allowed=allowedDays(c);Set<LocalDate> blackouts=blackoutDates(c);Map<LocalDate,Integer> dayCount=new HashMap<>();Map<LocalDateTime,Integer> slotCount=new HashMap<>();Map<Integer,ZonedDateTime> lastTeam=new HashMap<>();List<Instant> result=new ArrayList<>();ZonedDateTime cursor=c.getLeagueStartAt().atZone(zone);if(cursor.toLocalTime().isBefore(c.getDailyStartTime()))cursor=cursor.with(c.getDailyStartTime());long requiredRestHours=requiredRestHours(c);for(int[] pair:pairs){ZonedDateTime candidate=cursor;boolean placed=false;for(int attempt=0;attempt<200000;attempt++){LocalDate date=candidate.toLocalDate();LocalTime time=candidate.toLocalTime();if(!allowed.contains(candidate.getDayOfWeek())||blackouts.contains(date)||time.isBefore(c.getDailyStartTime())||time.isAfter(c.getDailyEndTime().minusMinutes(c.getFixtureSlotMinutes()))){candidate=nextSchedulingDay(candidate,c,allowed,blackouts);continue;}if(dayCount.getOrDefault(date,0)>=c.getMaximumFixturesPerDay()){candidate=nextSchedulingDay(candidate,c,allowed,blackouts);continue;}int capacity=Boolean.TRUE.equals(c.getSimultaneousFixturesAllowed())?Math.max(1,venueCount):1;if(slotCount.getOrDefault(candidate.toLocalDateTime(),0)>=capacity){candidate=candidate.plusMinutes(c.getFixtureSlotMinutes());continue;}ZonedDateTime left=lastTeam.get(pair[2]),right=lastTeam.get(pair[3]);if(!Boolean.TRUE.equals(c.getTeamMayPlayTwicePerDay())&&((left!=null&&left.toLocalDate().equals(date))||(right!=null&&right.toLocalDate().equals(date)))){candidate=nextSchedulingDay(candidate,c,allowed,blackouts);continue;}ZonedDateTime restLimit=java.util.stream.Stream.of(left,right).filter(Objects::nonNull).max(Comparator.naturalOrder()).map(x->x.plusHours(requiredRestHours)).orElse(null);if(restLimit!=null&&candidate.isBefore(restLimit)){candidate=restLimit;continue;}result.add(candidate.toInstant());dayCount.merge(date,1,Integer::sum);slotCount.merge(candidate.toLocalDateTime(),1,Integer::sum);lastTeam.put(pair[2],candidate);lastTeam.put(pair[3],candidate);cursor=candidate;placed=true;break;}if(!placed)throw new ResponseStatusException(HttpStatus.CONFLICT,"No valid schedule could be produced within the configured constraints");}return result;}
    private static ZonedDateTime nextSchedulingDay(ZonedDateTime value,CompetitionConfiguration c,Set<DayOfWeek> allowed,Set<LocalDate> blackouts){ZonedDateTime next=value.plusDays(1).with(c.getDailyStartTime());for(int i=0;i<370;i++){if(allowed.contains(next.getDayOfWeek())&&!blackouts.contains(next.toLocalDate()))return next;next=next.plusDays(1);}throw new ResponseStatusException(HttpStatus.CONFLICT,"No allowed scheduling date is available in the next year");}
    private static Set<DayOfWeek> allowedDays(CompetitionConfiguration c){Set<DayOfWeek> result=EnumSet.noneOf(DayOfWeek.class);for(String value:c.getAllowedWeekdays().split(","))if(!value.isBlank())result.add(DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT)));return result;}
    private static Set<LocalDate> blackoutDates(CompetitionConfiguration c){Set<LocalDate> result=new HashSet<>();if(c.getBlackoutDates()!=null)for(String value:c.getBlackoutDates().split("\\R"))if(!value.isBlank())result.add(LocalDate.parse(value.trim()));return result;}
    private List<String> validateScheduledAt(Instant at,UUID teamA,UUID teamB,CompetitionConfiguration c,List<Match> existing){List<String> errors=new ArrayList<>();ZonedDateTime local=at.atZone(ZoneId.of(c.getTimezone()));if(!allowedDays(c).contains(local.getDayOfWeek()))errors.add("Selected date is not an allowed weekday");if(blackoutDates(c).contains(local.toLocalDate()))errors.add("Selected date is blacked out");if(local.toLocalTime().isBefore(c.getDailyStartTime())||local.toLocalTime().isAfter(c.getDailyEndTime().minusMinutes(c.getFixtureSlotMinutes())))errors.add("Selected time is outside the configured scheduling window");long onDay=existing.stream().filter(x->x.getScheduledAt().atZone(ZoneId.of(c.getTimezone())).toLocalDate().equals(local.toLocalDate())).count();if(onDay>=c.getMaximumFixturesPerDay())errors.add("Maximum fixtures per day has been reached");long requiredRestHours=requiredRestHours(c);for(Match x:existing){boolean sameTeam=Set.of(teamA,teamB).contains(x.getTeamA().getId())||Set.of(teamA,teamB).contains(x.getTeamB().getId());ZonedDateTime other=x.getScheduledAt().atZone(ZoneId.of(c.getTimezone()));if(sameTeam&&!Boolean.TRUE.equals(c.getTeamMayPlayTwicePerDay())&&other.toLocalDate().equals(local.toLocalDate()))errors.add("A team may not play twice on the same day");if(sameTeam&&Math.abs(Duration.between(other,local).toHours())<requiredRestHours)errors.add("Minimum team rest and fixture spacing are not satisfied");if(!Boolean.TRUE.equals(c.getSimultaneousFixturesAllowed())&&x.getScheduledAt().equals(at))errors.add("Simultaneous fixtures are disabled");}return errors.stream().distinct().toList();}
    private static long requiredRestHours(CompetitionConfiguration c){return Math.max(Optional.ofNullable(c.getMinimumRestHours()).orElse(0),Optional.ofNullable(c.getFixtureSpacingDays()).orElse(0)*24L);}
    private void components(Match fixture,SportCompetitionEngine engine,CompetitionConfiguration config) {for(var item:engine.fixtureComponents(config)){MatchFormat format=formats.save(MatchFormat.builder().championship(fixture.getChampionship()).match(fixture).formatType(item.formatType()).formatOrder(item.order()).displayLabel(item.label()).shortLabel(item.shortLabel()).playersPerSide(item.playersPerSide()).genderRule(item.genderRule()).scoreA(0).scoreB(0).completed(false).build());fixture.getFormats().add(format);}}
    private CompetitionStage stage(Championship championship,CompetitionStageType type,int order){return stages.findForUpdate(championship.getId(),type).orElseGet(()->stages.save(CompetitionStage.builder().championship(championship).stageType(type).stageOrder(order).build()));}
    private boolean isAdmin(UUID cid,Authentication auth){if(auth==null||!(auth.getPrincipal() instanceof User user)||user.getRole()==Role.SUPER_ADMIN)return false;return roles.findByUserIdAndChampionshipId(user.getId(),cid).map(role->role.getRole()==ChampionshipRoleType.CHAMPIONSHIP_ADMIN).orElse(false);}
    private void audit(Championship championship,Match fixture,User actor,String type,int revision,Object payload){String reason=payload instanceof Map<?,?> map&&map.get("reason")!=null?String.valueOf(map.get("reason")):null;Integer configVersion=configs.findByChampionshipId(championship.getId()).map(CompetitionConfiguration::getConfigurationVersion).orElse(null);audits.save(CompetitionAuditEvent.builder().championship(championship).fixture(fixture).fixtureChampionshipId(fixture==null?null:championship.getId()).actor(actor).eventType(type).revision(Math.max(1,revision)).configurationVersion(configVersion).entityType(fixture==null?"CHAMPIONSHIP":"MATCH").entityId(fixture==null?championship.getId():fixture.getId()).reason(reason).correlationId(UUID.randomUUID()).payload(toJson(payload)).build());}
    private String toJson(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException exception){throw new IllegalStateException("Unable to serialize competition state",exception);}}
    private String join(List<String> values,String separator){return values.stream().map(String::trim).filter(value->!value.isBlank()).distinct().collect(Collectors.joining(separator));}
    private SportCompetitionEngine engine(Championship championship){return engines.require(championship.getSportType());}
    private Championship championship(UUID id){return championships.findById(id).orElseThrow(()->notFound("Championship"));}
    private Championship lockedChampionship(UUID id){return championships.findByIdForUpdate(id).orElseThrow(()->notFound("Championship"));}
    private CompetitionConfiguration config(UUID id){return configs.findByChampionshipId(id).orElseThrow(()->notFound("Competition configuration"));}
    private CompetitionConfiguration lockedConfig(UUID id){return configs.findByChampionshipIdForUpdate(id).orElseThrow(()->notFound("Competition configuration"));}
    private Match lockedFixture(UUID cid,UUID id){return fixtures.findByIdAndChampionshipIdForUpdate(id,cid).orElseThrow(()->notFound("Fixture"));}
    private void conflict(String message){throw new ResponseStatusException(HttpStatus.CONFLICT,message);}
    private ResponseStatusException notFound(String value){return new ResponseStatusException(HttpStatus.NOT_FOUND,value+" not found");}
}
