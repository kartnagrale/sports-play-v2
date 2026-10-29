package com.neml.badminton.competition;
import com.neml.badminton.entity.*;import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.*;
class CricketCompetitionEngineTest{
 private final CricketCompetitionEngine engine=new CricketCompetitionEngine();
 @Test void t20DefaultsAreCorporatePresetValues(){var d=engine.defaultConfiguration();assertThat(d.pointsPerWin()).isEqualTo(2);assertThat(d.qualifyingTeamCount()).isEqualTo(4);assertThat(d.tieBreakerPriority()).startsWith("NET_RUN_RATE");}
 @Test void topTwoFinalRequiresExactlyTwoQualifiers(){CompetitionConfiguration c=CompetitionConfiguration.builder().pointsPerWin(2).pointsPerLoss(0).pointsPerTie(1).pointsPerNoResult(1).fixtureSpacingDays(1).playoffFormat(PlayoffFormat.TOP_TWO_FINAL).qualifyingTeamCount(4).build();assertThatThrownBy(()->engine.validateConfiguration(c,8)).hasMessageContaining("requires 2 qualifiers");}
 @Test void equalPointsAndNetRunRateHaveStableNameTieBreakAcrossInputOrders(){
  Team alpha=Team.builder().id(java.util.UUID.randomUUID()).name("Alpha").build();
  Team zebra=Team.builder().id(java.util.UUID.randomUUID()).name("Zebra").build();
  var config=CompetitionConfiguration.builder().build();
  for(var teams:java.util.List.of(java.util.List.of(zebra,alpha),java.util.List.of(alpha,zebra))){
   var standings=engine.calculateStandings(config,TournamentSettings.builder().build(),teams,java.util.List.of(),java.util.Map.of(),false);
   assertThat(standings).extracting(s->s.team().name()).containsExactly("Alpha","Zebra");
   assertThat(standings).extracting(s->s.rank()).containsExactly(1,2);
  }
 }
}
