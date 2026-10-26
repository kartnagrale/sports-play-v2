package com.neml.badminton.competition;

import com.neml.badminton.dto.MatchDtos.*;
import com.neml.badminton.entity.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.neml.badminton.repository.BadmintonConfigurationRepository;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Component
public class BadmintonCompetitionEngine implements SportCompetitionEngine {
    private BadmintonConfigurationRepository configurations;
    public BadmintonCompetitionEngine() {}
    @Autowired public BadmintonCompetitionEngine(BadmintonConfigurationRepository configurations){this.configurations=configurations;}
    private static final List<RubberDefinition> RUBBERS = List.of(
            new RubberDefinition(FormatType.MENS_SINGLES, 1, "Men's Singles", "MS", 1, "MALE"),
            new RubberDefinition(FormatType.WOMENS_SINGLES, 2, "Women's Singles", "WS", 1, "FEMALE"),
            new RubberDefinition(FormatType.MENS_DOUBLES, 3, "Men's Doubles", "MD", 2, "MALE"),
            new RubberDefinition(FormatType.MIXED_DOUBLES, 4, "Mixed Doubles", "XD", 2, "MIXED"),
            new RubberDefinition(FormatType.MENS_DOUBLES_TWO, 5, "Men's Doubles II", "MD2", 2, "MALE")
    );

    @Override public String sportCode() { return "BADMINTON"; }

    @Override
    public CompetitionDefaults defaultConfiguration() {
        return new CompetitionDefaults(RoundRobinMode.SINGLE, 3, 0,
                "HEAD_TO_HEAD,FORMAT_WINS,FORMAT_DIFF,MATCHES_WON,NAME",
                4, 1, true, List.of("Center Court"));
    }

    @Override
    public void validateConfiguration(CompetitionConfiguration configuration, int teamCount) {
        if (configuration.getPointsPerWin() < 0 || configuration.getPointsPerLoss() < 0)
            bad("League points cannot be negative");
        if (configuration.getFixtureSpacingDays() < 0)
            bad("Fixture spacing cannot be negative");
        if (teamCount > 0 && configuration.getPlayoffFormat()!=PlayoffFormat.NONE && (configuration.getQualifyingTeamCount() < 2 ||
                configuration.getQualifyingTeamCount() > teamCount ||
                configuration.getQualifyingTeamCount() % 2 != 0))
            bad("Qualifying team count must be an even number between 2 and the number of teams");
        Set<String> supported = Set.of("HEAD_TO_HEAD", "FORMAT_WINS", "FORMAT_DIFF", "MATCHES_WON", "NAME");
        for (String value : tieBreakers(configuration))
            if (!supported.contains(value)) bad("Unsupported badminton tie-breaker: " + value);
        if (venues(configuration).isEmpty()) bad("At least one match venue is required");
    }

    @Override public List<RubberDefinition> fixtureComponents() { return RUBBERS; }

    @Override public List<RubberDefinition> fixtureComponents(CompetitionConfiguration configuration) {
        return persisted(configuration.getChampionship().getId()).map(c->c.getRubbers().stream().filter(r->Boolean.TRUE.equals(r.getEnabled()))
                .map(r->new RubberDefinition(FormatType.valueOf(r.getRubberCode()),r.getDisplayOrder(),r.getDisplayLabel(),r.getShortLabel(),r.getParticipantCountPerSide(),r.getGenderRule())).toList()).orElse(RUBBERS);
    }

    @Override public void validateLineup(MatchFormat format,List<Player> sideA,List<Player> sideB){
        RubberDefinition rule=definition(format.getMatch().getChampionship().getId(),format.getFormatType());validateSide(rule,sideA);validateSide(rule,sideB);
        BadmintonConfiguration config=persisted(format.getMatch().getChampionship().getId()).orElse(null);int maximum=config==null?1:config.getMaximumAppearancesPerPlayer();
        List<Player> selected=new ArrayList<>();selected.addAll(sideA);selected.addAll(sideB);for(Player player:selected){long appearances=format.getMatch().getFormats().stream().filter(f->!Objects.equals(f.getId(),format.getId()))
                .filter(f->f.getSideAPlayers().stream().anyMatch(p->p.getId().equals(player.getId()))||f.getSideBPlayers().stream().anyMatch(p->p.getId().equals(player.getId()))).count();
            if(config!=null&&!Boolean.TRUE.equals(config.getMultipleRubberCategoriesAllowed())&&appearances>0)bad("A player may not appear in multiple rubber categories");
            if(appearances>=maximum)bad("A player may appear in at most "+maximum+" rubbers per fixture");}
    }

    @Override public boolean lineupChangesAfterScoringAllowed(UUID championshipId) {
        return persisted(championshipId).map(BadmintonConfiguration::getLineupChangesAfterStartAllowed).orElse(false);
    }

    @Override
    public void validateLineup(FormatType type, List<Player> sideA, List<Player> sideB) {
        RubberDefinition rule = RUBBERS.stream().filter(item -> item.formatType() == type).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported badminton rubber"));
        validateSide(rule, sideA);
        validateSide(rule, sideB);
    }

    private void validateSide(RubberDefinition rule, List<Player> players) {
        if (players == null || players.size() != rule.playersPerSide())
            bad(rule.label() + " requires " + rule.playersPerSide() + " player(s) per team");
        if (players.stream().map(Player::getId).distinct().count() != players.size())
            bad("A player cannot occupy multiple slots in the same rubber");
        switch (rule.genderRule()) {
            case "MALE" -> {
                if (players.stream().anyMatch(player -> player.getGender() != Gender.MALE))
                    bad(rule.label() + " requires male players");
            }
            case "FEMALE" -> {
                if (players.stream().anyMatch(player -> player.getGender() != Gender.FEMALE))
                    bad(rule.label() + " requires female players");
            }
            case "MIXED" -> {
                if (players.stream().filter(player -> player.getGender() == Gender.MALE).count() != 1 ||
                        players.stream().filter(player -> player.getGender() == Gender.FEMALE).count() != 1)
                    bad(rule.label() + " requires one male and one female player");
            }
            case "OPEN" -> { }
            default -> throw new IllegalStateException("Unknown gender rule " + rule.genderRule());
        }
    }

    @Override
    public void validateScore(MatchFormat format, int scoreA, int scoreB, boolean finalResult) {
        if (scoreA < 0 || scoreB < 0) bad("Scores cannot be negative");
        if (finalResult && scoreA == scoreB) bad("Completed badminton rubber scores cannot be tied");
        if (finalResult && scoreA == 0 && scoreB == 0) bad("A completed badminton rubber requires a score");
        if(finalResult){BadmintonRubberRule rule=persisted(format.getMatch().getChampionship().getId()).flatMap(c->c.getRubbers().stream().filter(r->r.getRubberCode().equals(format.getFormatType().name())).findFirst()).orElse(null);
            if(rule!=null){int winner=Math.max(scoreA,scoreB),loser=Math.min(scoreA,scoreB);if(winner>rule.getScoreCap()||winner<rule.getPointsPerGame()||(winner<rule.getScoreCap()&&winner-loser<rule.getWinBy()))bad("Score must reach "+rule.getPointsPerGame()+", win by "+rule.getWinBy()+", with a cap of "+rule.getScoreCap());}}
    }

    @Override
    public MatchOutcome calculateMatchOutcome(Match fixture) {
        int sideA = 0, sideB = 0, completed = 0;
        for (MatchFormat format : fixture.getFormats()) {
            if (!Boolean.TRUE.equals(format.getCompleted())) continue;
            completed++;
            if (format.getWinnerTeam() != null && format.getWinnerTeam().getId().equals(fixture.getTeamA().getId())) sideA++;
            else if (format.getWinnerTeam() != null && format.getWinnerTeam().getId().equals(fixture.getTeamB().getId())) sideB++;
        }
        boolean finished = !fixture.getFormats().isEmpty() && completed == fixture.getFormats().size();
        if(finished&&sideA==sideB&&persisted(fixture.getChampionship().getId()).map(c->"ADMINISTRATIVE_WIN".equals(c.getTieHandling())).orElse(false))finished=false;
        return new MatchOutcome(finished && sideA!=sideB ? (sideA > sideB ? fixture.getTeamA() : fixture.getTeamB()) : null,
                sideA, sideB, finished);
    }

    @Override
    public List<StandingDto> calculateStandings(CompetitionConfiguration configuration,
                                                TournamentSettings legacyRules,
                                                List<Team> teams,
                                                List<Match> leagueFixtures,
                                                Map<UUID, List<UUID>> unplayed,
                                                boolean applyPenalties) {
        Map<UUID, int[]> stats = new HashMap<>(); // played, won, lost, rubber wins, rubber losses, points
        Map<UUID, Map<UUID, Integer>> headToHead = new HashMap<>();
        teams.forEach(team -> {
            stats.put(team.getId(), new int[6]);
            headToHead.put(team.getId(), new HashMap<>());
        });
        for (Match fixture : leagueFixtures) {
            if (fixture.getStatus() != MatchStatus.COMPLETED) continue;
            UUID a = fixture.getTeamA().getId(), b = fixture.getTeamB().getId();
            if (!stats.containsKey(a) || !stats.containsKey(b)) continue;
            int[] as = stats.get(a), bs = stats.get(b);
            as[0]++; bs[0]++;
            as[3] += fixture.getTeamAFormatWins(); as[4] += fixture.getTeamBFormatWins();
            bs[3] += fixture.getTeamBFormatWins(); bs[4] += fixture.getTeamAFormatWins();
            as[5] += fixture.getTeamAPoints(); bs[5] += fixture.getTeamBPoints();
            if (fixture.getWinnerTeam() == null) {
                // Even-rubber ties count as played and retain their backend-assigned tie points.
            } else if (fixture.getWinnerTeam().getId().equals(a)) {
                as[1]++; bs[2]++; headToHead.get(a).merge(b, 1, Integer::sum);
            } else {
                bs[1]++; as[2]++; headToHead.get(b).merge(a, 1, Integer::sum);
            }
        }
        boolean competitionStarted = leagueFixtures.stream().anyMatch(match -> match.getStatus() == MatchStatus.COMPLETED);
        List<StandingDto> table = new ArrayList<>();
        for (Team team : teams) {
            int[] value = stats.get(team.getId());
            int base = value[5];
            List<UUID> absent = unplayed.getOrDefault(team.getId(), List.of());
            int penalty = applyPenalties && competitionStarted ? absent.size() * legacyRules.getPenaltyPerUnplayed() : 0;
            table.add(new StandingDto(TeamRef.from(team), value[0], value[1], value[2], value[3], value[4],
                    value[3] - value[4], base, penalty, base - penalty, null,0, absent));
        }
        List<String> priority = tieBreakers(configuration);
        table.sort((left, right) -> compare(left, right, priority, headToHead));
        List<StandingDto> ranked = new ArrayList<>();
        for (int index = 0; index < table.size(); index++) {
            StandingDto row = table.get(index);
            ranked.add(new StandingDto(row.team(), row.played(), row.won(), row.lost(), row.formatWins(),
                    row.formatLosses(), row.formatDiff(), row.basePoints(), row.penalty(), row.totalPoints(),
                    row.netRunRate(),index + 1, row.unplayedPlayerIds()));
        }
        return ranked;
    }

    private int compare(StandingDto left, StandingDto right, List<String> priority,
                        Map<UUID, Map<UUID, Integer>> headToHead) {
        int compared = Integer.compare(right.totalPoints(), left.totalPoints());
        if (compared != 0) return compared;
        for (String rule : priority) {
            compared = switch (rule) {
                case "HEAD_TO_HEAD" -> Integer.compare(
                        headToHead.getOrDefault(right.team().id(), Map.of()).getOrDefault(left.team().id(), 0),
                        headToHead.getOrDefault(left.team().id(), Map.of()).getOrDefault(right.team().id(), 0));
                case "FORMAT_WINS" -> Integer.compare(right.formatWins(), left.formatWins());
                case "FORMAT_DIFF" -> Integer.compare(right.formatDiff(), left.formatDiff());
                case "MATCHES_WON" -> Integer.compare(right.won(), left.won());
                case "NAME" -> left.team().name().compareToIgnoreCase(right.team().name());
                default -> 0;
            };
            if (compared != 0) return compared;
        }
        return left.team().id().compareTo(right.team().id());
    }

    @Override
    public List<SeedPair> playoffPairings(int qualifierCount) {
        if (qualifierCount == 4) return List.of(new SeedPair(1, 4), new SeedPair(2, 3));
        if (qualifierCount == 2) return List.of(new SeedPair(1, 2));
        bad("Badminton playoffs currently support two or four qualifiers");
        return List.of();
    }

    @Override
    public List<String> supportedWidgets() {
        return List.of("TEAM_MATCH_STANDINGS", "RUBBER_RECORD", "FORMAT_LEADERS",
                "PLAYER_PARTICIPATION", "FORMAT_STRENGTH", "UPCOMING_FIXTURES", "PLAYOFF_BRACKET");
    }

    @Override
    public Map<String, String> terminology() {
        return Map.of("fixture", "Team match", "segment", "Rubber", "segments", "Rubbers",
                "score", "Rubber score", "standings", "League table");
    }

    @Override
    public Map<String, Object> dashboardMetrics(List<StandingDto> standings, List<Match> fixtures) {
        int completed = (int) fixtures.stream().filter(match -> match.getStatus() == MatchStatus.COMPLETED).count();
        int rubberWins = fixtures.stream().mapToInt(match -> n(match.getTeamAFormatWins()) + n(match.getTeamBFormatWins())).sum();
        return Map.of("completedTeamMatches", completed, "completedRubbers", rubberWins,
                "leader", standings.isEmpty() ? "" : standings.get(0).team().name());
    }

    public static List<String> venues(CompetitionConfiguration configuration) {
        return Arrays.stream(Optional.ofNullable(configuration.getVenues()).orElse("").split("\\R"))
                .map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
    }

    private List<String> tieBreakers(CompetitionConfiguration configuration) {
        return Arrays.stream(Optional.ofNullable(configuration.getTieBreakerPriority()).orElse("").split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT)).filter(value -> !value.isBlank()).toList();
    }

    private int n(Integer value) { return value == null ? 0 : value; }
    private Optional<BadmintonConfiguration> persisted(UUID cid){return configurations==null?Optional.empty():configurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(cid);}
    private RubberDefinition definition(UUID cid,FormatType type){return persisted(cid).flatMap(c->c.getRubbers().stream().filter(r->r.getRubberCode().equals(type.name())&&Boolean.TRUE.equals(r.getEnabled())).findFirst())
            .map(r->new RubberDefinition(type,r.getDisplayOrder(),r.getDisplayLabel(),r.getShortLabel(),r.getParticipantCountPerSide(),r.getGenderRule()))
            .orElseGet(()->RUBBERS.stream().filter(r->r.formatType()==type).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported badminton rubber")));}
    private void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
