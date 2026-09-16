package com.neml.badminton.dto;

import com.neml.badminton.competition.SportCompetitionEngine.RubberDefinition;
import com.neml.badminton.entity.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

import static com.neml.badminton.dto.MatchDtos.*;

public class CompetitionDtos {
    public record CompetitionConfigurationRequest(
            Integer expectedConfigurationVersion,
            Integer configuredTeamCount, Integer minimumTeamCount, Integer maximumTeamCount,
            Integer minimumRosterSize, Integer maximumRosterSize, Integer playingSideSize,
            Integer maximumSubstitutes, Integer numberOfLegs, PlayoffFormat playoffFormat,
            @NotNull RoundRobinMode roundRobinMode,
            @NotNull @Min(0) Integer pointsPerWin,
            @NotNull @Min(0) Integer pointsPerLoss,
            @Min(0) Integer pointsPerTie,
            @Min(0) Integer pointsPerNoResult,
            @NotEmpty List<@NotBlank String> tieBreakerPriority,
            @NotNull @Min(2) Integer qualifyingTeamCount,
            @NotEmpty List<@NotBlank @Size(max = 255) String> venues,
            @NotNull Instant leagueStartAt,
            @NotNull @Min(0) @Max(30) Integer fixtureSpacingDays,
            @NotNull Boolean manualDateEditing, String timezone, String presetCode,
            List<String> allowedWeekdays, LocalTime dailyStartTime, LocalTime dailyEndTime,
            Integer fixtureSlotMinutes,Integer maximumFixturesPerDay,List<String> blackoutDates,
            Integer minimumRestHours,Boolean simultaneousFixturesAllowed,Boolean teamMayPlayTwicePerDay) {}

    public record CompetitionConfigurationDto(
            UUID championshipId, String sportCode, Integer configurationVersion, ConfigurationStatus configurationStatus,
            Integer configuredTeamCount, Integer minimumTeamCount, Integer maximumTeamCount,
            Integer minimumRosterSize, Integer maximumRosterSize, Integer playingSideSize,
            Integer maximumSubstitutes, Integer numberOfLegs, PlayoffFormat playoffFormat,
            RoundRobinMode roundRobinMode,
            Integer pointsPerWin, Integer pointsPerLoss, List<String> tieBreakerPriority,
            Integer qualifyingTeamCount, List<String> venues, Instant leagueStartAt,
            Integer fixtureSpacingDays, Boolean manualDateEditing, Integer resultSchemaVersion,
            Integer pointsPerTie, Integer pointsPerNoResult, String timezone, String presetCode,
            List<String> allowedWeekdays,LocalTime dailyStartTime,LocalTime dailyEndTime,Integer fixtureSlotMinutes,
            Integer maximumFixturesPerDay,List<String> blackoutDates,Integer minimumRestHours,
            Boolean simultaneousFixturesAllowed,Boolean teamMayPlayTwicePerDay,
            TeamRef champion, TeamRef runnerUp) {}

    public record StageDto(UUID id, CompetitionStageType type, Integer order,
                           CompetitionStageStatus status, Instant startedAt, Instant completedAt,
                           Instant frozenAt) {
        public static StageDto from(CompetitionStage stage) {
            return new StageDto(stage.getId(), stage.getStageType(), stage.getStageOrder(), stage.getStatus(),
                    stage.getStartedAt(), stage.getCompletedAt(), stage.getFrozenAt());
        }
    }

    public record QualificationDto(Integer seed, TeamRef team, CompetitionStageType sourceStage) {
        public static QualificationDto from(CompetitionQualification qualification) {
            return new QualificationDto(qualification.getSeed(), TeamRef.from(qualification.getTeam()),
                    qualification.getSourceStage());
        }
    }

    public record AuditEventDto(UUID id, String eventType, UUID fixtureId, String actorName,
                                Integer revision,Integer configurationVersion,String entityType,UUID entityId,String reason,UUID correlationId,String payload, Instant occurredAt) {
        public static AuditEventDto from(CompetitionAuditEvent event) {
            return new AuditEventDto(event.getId(), event.getEventType(),
                    event.getFixture() == null ? null : event.getFixture().getId(),
                    event.getActor() == null ? "System" : event.getActor().getFullName(),
                    event.getRevision(),event.getConfigurationVersion(),event.getEntityType(),event.getEntityId(),event.getReason(),event.getCorrelationId(),event.getPayload(), event.getOccurredAt());
        }
    }

    public record CompetitionOverviewDto(CompetitionConfigurationDto configuration,
                                         List<StageDto> stages,
                                         List<QualificationDto> qualifications,
                                         List<MatchDto> fixtures,
                                         boolean leagueComplete,
                                         boolean playoffsGenerated,
                                         boolean finalGenerated) {}

    public record DashboardCapabilitiesDto(
            String sportCode, ChampionshipPhase currentPhase, List<String> permittedActions,
            List<String> supportedWidgets, Map<String, String> terminology,
            Map<String, Object> metrics, boolean configurationComplete,
            List<String> blockersForNextPhase, int teamCount, int playerCount,
            int lockedSquads, int totalSquads, List<MatchDto> upcomingFixtures,
            List<MatchDto> recentFixtures, List<StandingDto> standings,
            TeamRef champion, TeamRef runnerUp) {}

    public record SportCapabilitiesDto(String sportCode, List<RubberDefinition> fixtureComponents,
                                       List<String> supportedWidgets, Map<String, String> terminology) {}

    public record RescheduleFixtureRequest(@NotNull Instant scheduledAt, @Size(max = 255) String venue) {}
    public record ConfigurationActionRequest(Integer expectedVersion,@NotBlank @Size(max=500) String reason) {}
    public record ConfigurationFieldSchema(String name,String type,Object minimum,Object maximum,List<String> allowedValues,String dependency) {}
    public record ConfigurationSectionDto(String code,ConfigurationStatus status,boolean editable,Integer version,String lastChangedBy,Instant lastChangedAt,Instant lockedAt,List<String> dependencies,List<String> validationMessages,List<ConfigurationFieldSchema> fields) {}
    public record ConfigurationWorkspaceDto(Integer version,List<String> supportedPresets,List<ConfigurationSectionDto> sections,List<String> permittedActions,List<String> blockersForNextPhase) {}
    public record PhaseReadinessDto(ChampionshipPhase currentPhase,ChampionshipPhase targetPhase,boolean ready,List<String> blockers) {}

    public static CompetitionConfigurationDto from(CompetitionConfiguration config) {
        return new CompetitionConfigurationDto(config.getChampionship().getId(), config.getSportCode(),
                config.getConfigurationVersion(),config.getConfigurationStatus(),config.getConfiguredTeamCount(),
                config.getMinimumTeamCount(),config.getMaximumTeamCount(),config.getMinimumRosterSize(),
                config.getMaximumRosterSize(),config.getPlayingSideSize(),config.getMaximumSubstitutes(),
                config.getNumberOfLegs(),config.getPlayoffFormat(),
                config.getRoundRobinMode(), config.getPointsPerWin(), config.getPointsPerLoss(),
                split(config.getTieBreakerPriority(), ","), config.getQualifyingTeamCount(),
                split(config.getVenues(), "\n"), config.getLeagueStartAt(), config.getFixtureSpacingDays(),
                config.getManualDateEditing(), config.getResultSchemaVersion(),config.getPointsPerTie(),
                config.getPointsPerNoResult(),config.getTimezone(),config.getPresetCode(),
                split(config.getAllowedWeekdays(),","),config.getDailyStartTime(),config.getDailyEndTime(),config.getFixtureSlotMinutes(),
                config.getMaximumFixturesPerDay(),split(config.getBlackoutDates(),"\n"),config.getMinimumRestHours(),
                config.getSimultaneousFixturesAllowed(),config.getTeamMayPlayTwicePerDay(),
                TeamRef.from(config.getChampion()), TeamRef.from(config.getRunnerUp()));
    }

    private static List<String> split(String value, String separator) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(separator)).map(String::trim).filter(item -> !item.isBlank()).toList();
    }
}
