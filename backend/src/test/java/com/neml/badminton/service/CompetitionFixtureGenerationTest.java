package com.neml.badminton.service;

import com.neml.badminton.entity.RoundRobinMode;
import com.neml.badminton.entity.CompetitionConfiguration;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

class CompetitionFixtureGenerationTest {
    @Test
    void fourTeamSingleRoundRobinProducesExactlySixUniqueFixtures() {
        List<int[]> fixtures=CompetitionService.roundRobinIndexes(4,RoundRobinMode.SINGLE);
        Set<String> pairs=new HashSet<>();
        fixtures.forEach(pair->pairs.add(Math.min(pair[2],pair[3])+"-"+Math.max(pair[2],pair[3])));

        assertThat(fixtures).hasSize(6);
        assertThat(pairs).hasSize(6);
        assertThat(fixtures).allSatisfy(pair->assertThat(pair[2]).isNotEqualTo(pair[3]));
    }

    @Test
    void doubleRoundRobinMirrorsEveryPairAndProducesTwelveFixtures() {
        List<int[]> fixtures=CompetitionService.roundRobinIndexes(4,RoundRobinMode.DOUBLE);
        assertThat(fixtures).hasSize(12);
        assertThat(fixtures.stream().filter(pair->pair[1]==1)).hasSize(6);
        assertThat(fixtures.stream().filter(pair->pair[1]==2)).hasSize(6);
    }

    @Test
    void acceptanceFixtureCountsAreExact() {
        assertThat(CompetitionService.roundRobinIndexes(6,RoundRobinMode.SINGLE)).hasSize(15);
        assertThat(CompetitionService.roundRobinIndexes(8,RoundRobinMode.SINGLE)).hasSize(28);
    }

    @Test
    void oddTeamRoundRobinUsesByesWithoutDuplicateOrSelfFixtures() {
        List<int[]> fixtures=CompetitionService.roundRobinIndexes(5,RoundRobinMode.SINGLE);
        Set<String> pairs=new HashSet<>();
        fixtures.forEach(pair->pairs.add(Math.min(pair[2],pair[3])+"-"+Math.max(pair[2],pair[3])));

        assertThat(fixtures).hasSize(10);
        assertThat(pairs).hasSize(10);
        assertThat(fixtures).allSatisfy(pair->assertThat(pair[2]).isNotEqualTo(pair[3]));
    }

    @Test
    void customLegCountMirrorsTeamsAndScalesFixtureCount() {
        CompetitionConfiguration config=CompetitionConfiguration.builder()
                .roundRobinMode(RoundRobinMode.CUSTOM).numberOfLegs(3).build();
        List<int[]> fixtures=CompetitionService.roundRobinIndexes(4,config);

        assertThat(fixtures).hasSize(18);
        assertThat(fixtures.stream().mapToInt(pair->pair[1]).distinct()).containsExactly(1,2,3);
    }

    @Test
    void schedulerHonorsAllowedDaysBlackoutsDailyCapsAndTeamRest() {
        CompetitionConfiguration config=CompetitionConfiguration.builder()
                .timezone("Asia/Kolkata").leagueStartAt(Instant.parse("2026-09-04T03:30:00Z"))
                .allowedWeekdays("SATURDAY,SUNDAY").dailyStartTime(LocalTime.of(9,0))
                .dailyEndTime(LocalTime.of(17,0)).fixtureSlotMinutes(120).maximumFixturesPerDay(2)
                .blackoutDates("2026-09-05").minimumRestHours(12)
                .simultaneousFixturesAllowed(false).teamMayPlayTwicePerDay(false).build();
        List<int[]> pairs=CompetitionService.roundRobinIndexes(4,RoundRobinMode.SINGLE);

        List<Instant> scheduled=CompetitionService.scheduleTimes(pairs,config,2);

        ZoneId zone=ZoneId.of("Asia/Kolkata");
        assertThat(scheduled).hasSize(6).allSatisfy(at->{
            ZonedDateTime local=at.atZone(zone);
            assertThat(local.getDayOfWeek()).isIn(DayOfWeek.SATURDAY,DayOfWeek.SUNDAY);
            assertThat(local.toLocalDate()).isNotEqualTo(LocalDate.of(2026,9,5));
            assertThat(local.toLocalTime()).isBetween(LocalTime.of(9,0),LocalTime.of(15,0));
        });
        assertThat(scheduled.stream().collect(java.util.stream.Collectors.groupingBy(at->at.atZone(zone).toLocalDate(),java.util.stream.Collectors.counting())).values())
                .allSatisfy(count->assertThat(count).isLessThanOrEqualTo(2));
        Map<Integer,Instant> lastByTeam=new HashMap<>();
        for(int index=0;index<pairs.size();index++){
            Instant at=scheduled.get(index);
            for(int team:new int[]{pairs.get(index)[2],pairs.get(index)[3]}){
                if(lastByTeam.containsKey(team))assertThat(Duration.between(lastByTeam.get(team),at).abs()).isGreaterThanOrEqualTo(Duration.ofDays(1));
                lastByTeam.put(team,at);
            }
        }
    }
}
