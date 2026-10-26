package com.neml.badminton.competition;
import com.neml.badminton.entity.*;import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.*;
class CricketCompetitionEngineTest{
 private final CricketCompetitionEngine engine=new CricketCompetitionEngine();
 @Test void t20DefaultsAreCorporatePresetValues(){var d=engine.defaultConfiguration();assertThat(d.pointsPerWin()).isEqualTo(2);assertThat(d.qualifyingTeamCount()).isEqualTo(4);assertThat(d.tieBreakerPriority()).startsWith("NET_RUN_RATE");}
 @Test void topTwoFinalRequiresExactlyTwoQualifiers(){CompetitionConfiguration c=CompetitionConfiguration.builder().pointsPerWin(2).pointsPerLoss(0).pointsPerTie(1).pointsPerNoResult(1).fixtureSpacingDays(1).playoffFormat(PlayoffFormat.TOP_TWO_FINAL).qualifyingTeamCount(4).build();assertThatThrownBy(()->engine.validateConfiguration(c,8)).hasMessageContaining("requires 2 qualifiers");}
}
