package com.neml.badminton.competition;

import com.neml.badminton.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class BadmintonCompetitionEngineTest {
    private final BadmintonCompetitionEngine engine=new BadmintonCompetitionEngine();

    @Test
    void validatesRubberGenderCompositionAndDuplicateSlots() {
        Player man=player(Gender.MALE),woman=player(Gender.FEMALE);
        assertThatCode(()->engine.validateLineup(FormatType.MIXED_DOUBLES,List.of(man,woman),
                List.of(player(Gender.MALE),player(Gender.FEMALE)))).doesNotThrowAnyException();
        assertThatThrownBy(()->engine.validateLineup(FormatType.MIXED_DOUBLES,List.of(man,player(Gender.MALE)),
                List.of(player(Gender.MALE),player(Gender.FEMALE)))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->engine.validateLineup(FormatType.MENS_DOUBLES,List.of(man,man),
                List.of(player(Gender.MALE),player(Gender.MALE)))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void winnerCalculationRequiresEveryRubberAndCannotTie() {
        Team a=Team.builder().id(UUID.randomUUID()).build(),b=Team.builder().id(UUID.randomUUID()).build();
        Match match=Match.builder().teamA(a).teamB(b).formats(new ArrayList<>()).build();
        for(int index=0;index<5;index++) {
            match.getFormats().add(MatchFormat.builder().completed(true).winnerTeam(index<3?a:b).build());
        }
        var outcome=engine.calculateMatchOutcome(match);
        assertThat(outcome.completed()).isTrue();
        assertThat(outcome.winner()).isEqualTo(a);
        assertThat(outcome.sideAWins()).isEqualTo(3);
        assertThatThrownBy(()->engine.validateScore(match.getFormats().get(0),11,11,true))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void fourTeamSeedingIsOneVsFourAndTwoVsThree() {
        assertThat(engine.playoffPairings(4)).containsExactly(
                new SportCompetitionEngine.SeedPair(1,4),
                new SportCompetitionEngine.SeedPair(2,3));
    }

    private Player player(Gender gender) {
        return Player.builder().id(UUID.randomUUID()).gender(gender).status(PlayerStatus.SOLD).build();
    }
}
