package com.neml.badminton.service;
import com.neml.badminton.entity.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class CricketConfigurablePointsTest {
    Team a=Team.builder().id(UUID.randomUUID()).build(),b=Team.builder().id(UUID.randomUUID()).build();
    Match match=Match.builder().teamA(a).teamB(b).build();
    @Test void customWinAndLossPointsAreAppliedToBothSides(){
        CricketScoringService.applyPoints(match,b,3,1,2);
        assertThat(match.getTeamAPoints()).isEqualTo(1);assertThat(match.getTeamBPoints()).isEqualTo(3);
    }
    @Test void configuredDrawPointsAndNplExamplesArePreserved(){
        CricketScoringService.applyPoints(match,null,2,0,1);
        assertThat(match.getTeamAPoints()).isEqualTo(1);assertThat(match.getTeamBPoints()).isEqualTo(1);
        CricketScoringService.applyPoints(match,a,2,0,1);
        assertThat(match.getTeamAPoints()).isEqualTo(2);assertThat(match.getTeamBPoints()).isZero();
    }
}
