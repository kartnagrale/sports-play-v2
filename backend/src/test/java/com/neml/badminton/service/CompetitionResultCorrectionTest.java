package com.neml.badminton.service;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitionResultCorrectionTest {
    @Mock ChampionshipRepository championships;
    @Mock CompetitionConfigurationRepository configs;
    @Mock CompetitionStageRepository stages;
    @Mock MatchRepository fixtures;
    @InjectMocks CompetitionService service;
    final UUID cid=UUID.randomUUID(),mid=UUID.randomUUID();

    @Test void correctingFinalClearsAwardsAndReopensChampionshipAndStage(){
        Championship championship=Championship.builder().id(cid).status(ChampionshipStatus.COMPLETED).lifecyclePhase(ChampionshipPhase.COMPLETED).build();
        Match match=Match.builder().id(mid).championship(championship).stageType(CompetitionStageType.FINAL).status(MatchStatus.LIVE).build();
        CompetitionConfiguration config=CompetitionConfiguration.builder().championship(championship).champion(new Team()).runnerUp(new Team()).build();
        CompetitionStage stage=CompetitionStage.builder().stageType(CompetitionStageType.FINAL).status(CompetitionStageStatus.COMPLETED).completedAt(Instant.now()).build();
        when(championships.findByIdForUpdate(cid)).thenReturn(Optional.of(championship));
        when(fixtures.findByIdAndChampionshipIdForUpdate(mid,cid)).thenReturn(Optional.of(match));
        when(configs.findByChampionshipIdForUpdate(cid)).thenReturn(Optional.of(config));
        when(stages.findForUpdate(cid,CompetitionStageType.FINAL)).thenReturn(Optional.of(stage));
        service.reopenAfterCorrection(cid,mid);
        assertThat(config.getChampion()).isNull();assertThat(config.getRunnerUp()).isNull();
        assertThat(championship.getStatus()).isEqualTo(ChampionshipStatus.ACTIVE);
        assertThat(championship.getLifecyclePhase()).isEqualTo(ChampionshipPhase.FINAL);
        assertThat(stage.getStatus()).isEqualTo(CompetitionStageStatus.ACTIVE);assertThat(stage.getCompletedAt()).isNull();
        verify(configs).save(config);verify(championships).save(championship);verify(stages).save(stage);
    }
    @Test void topTwoFinalFreezesLeagueEvenWithoutSemifinals(){
        when(fixtures.existsByChampionshipIdAndStageType(eq(cid),any())).thenAnswer(call->call.getArgument(1)==CompetitionStageType.FINAL);
        assertThat(service.hasDownstreamFixture(cid,CompetitionStageType.LEAGUE)).isTrue();
        assertThat(service.hasDownstreamFixture(cid,CompetitionStageType.FINAL)).isFalse();
    }
    @Test void leagueWithoutAnyPlayoffsRemainsCorrectable(){
        assertThat(service.hasDownstreamFixture(cid,CompetitionStageType.LEAGUE)).isFalse();
    }
}
