package com.neml.badminton.competition;

import com.neml.badminton.dto.MatchDtos.StandingDto;
import com.neml.badminton.entity.*;

import java.util.*;

public interface SportCompetitionEngine {
    String sportCode();

    CompetitionDefaults defaultConfiguration();

    void validateConfiguration(CompetitionConfiguration configuration, int teamCount);

    List<RubberDefinition> fixtureComponents();
    default List<RubberDefinition> fixtureComponents(CompetitionConfiguration configuration) { return fixtureComponents(); }

    void validateLineup(FormatType formatType, List<Player> sideA, List<Player> sideB);
    default void validateLineup(MatchFormat format,List<Player> sideA,List<Player> sideB){validateLineup(format.getFormatType(),sideA,sideB);}
    default boolean lineupChangesAfterScoringAllowed(UUID championshipId) { return false; }

    void validateScore(MatchFormat format, int scoreA, int scoreB, boolean finalResult);

    MatchOutcome calculateMatchOutcome(Match fixture);

    List<StandingDto> calculateStandings(CompetitionConfiguration configuration,
                                         TournamentSettings legacyRules,
                                         List<Team> teams,
                                         List<Match> leagueFixtures,
                                         Map<UUID, List<UUID>> unplayedPlayerIds,
                                         boolean applyParticipationPenalties);

    List<SeedPair> playoffPairings(int qualifierCount);

    List<String> supportedWidgets();

    Map<String, String> terminology();

    Map<String, Object> dashboardMetrics(List<StandingDto> standings, List<Match> fixtures);

    record CompetitionDefaults(RoundRobinMode roundRobinMode, int pointsPerWin, int pointsPerLoss,
                               String tieBreakerPriority, int qualifyingTeamCount, int fixtureSpacingDays,
                               boolean manualDateEditing, List<String> venues) {}

    record RubberDefinition(FormatType formatType, int order, String label, String shortLabel,
                            int playersPerSide, String genderRule) {}

    record MatchOutcome(Team winner, int sideAWins, int sideBWins, boolean completed) {}

    record SeedPair(int seedA, int seedB) {}
}
