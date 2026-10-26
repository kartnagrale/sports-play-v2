package com.neml.badminton.service;
import com.neml.badminton.entity.CricketDelivery;
import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
class CricketScoringRulesTest{
 @Test void widesAndNoBallsAreNotLegalDeliveries(){assertThat(CricketScoringService.isLegal(1,0)).isFalse();assertThat(CricketScoringService.isLegal(0,1)).isFalse();assertThat(CricketScoringService.isLegal(0,0)).isTrue();}
 @Test void deliveryTotalIncludesEveryBackendCalculatedExtra(){assertThat(CricketScoringService.deliveryRuns(4,1,0,0,0,5)).isEqualTo(10);}
 @Test void oversAreFormattedFromIntegerLegalBalls(){assertThat(CricketScoringService.formatOvers(19)).isEqualTo("3.1");assertThat(CricketScoringService.formatOvers(24)).isEqualTo("4.0");}
 @Test void freeHitSurvivesIllegalBallsAndEndsAfterTheNextLegalBall(){CricketDelivery noBall=CricketDelivery.builder().noBalls(1).legalBall(false).build();CricketDelivery wide=CricketDelivery.builder().noBalls(0).legalBall(false).build();CricketDelivery legal=CricketDelivery.builder().noBalls(0).legalBall(true).build();assertThat(CricketScoringService.freeHitDue(List.of(noBall,wide))).isTrue();assertThat(CricketScoringService.freeHitDue(List.of(noBall,wide,legal))).isFalse();}
}
