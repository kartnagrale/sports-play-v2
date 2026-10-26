package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "competition_configurations",
        uniqueConstraints = @UniqueConstraint(name = "uq_competition_config_championship", columnNames = "championship_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionConfiguration {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @Column(name = "configuration_version", nullable = false) @Builder.Default private Integer configurationVersion = 1;
    @Enumerated(EnumType.STRING) @Column(name = "configuration_status", nullable = false, length = 16)
    @Builder.Default private ConfigurationStatus configurationStatus = ConfigurationStatus.DRAFT;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false, unique = true)
    private Championship championship;

    @Column(name = "sport_code", nullable = false, length = 64)
    private String sportCode;

    @Column(name = "configured_team_count", nullable = false) @Builder.Default private Integer configuredTeamCount = 4;
    @Column(name = "minimum_team_count", nullable = false) @Builder.Default private Integer minimumTeamCount = 2;
    @Column(name = "maximum_team_count", nullable = false) @Builder.Default private Integer maximumTeamCount = 4;
    @Column(name = "minimum_roster_size", nullable = false) @Builder.Default private Integer minimumRosterSize = 8;
    @Column(name = "maximum_roster_size", nullable = false) @Builder.Default private Integer maximumRosterSize = 12;
    @Column(name = "playing_side_size", nullable = false) @Builder.Default private Integer playingSideSize = 7;
    @Column(name = "maximum_substitutes", nullable = false) @Builder.Default private Integer maximumSubstitutes = 0;
    @Column(name = "number_of_legs", nullable = false) @Builder.Default private Integer numberOfLegs = 1;
    @Enumerated(EnumType.STRING) @Column(name = "playoff_format", nullable = false, length = 48)
    @Builder.Default private PlayoffFormat playoffFormat = PlayoffFormat.SEMI_FINALS_AND_FINAL;
    @Column(name = "points_per_tie", nullable = false) @Builder.Default private Integer pointsPerTie = 1;
    @Column(name = "points_per_no_result", nullable = false) @Builder.Default private Integer pointsPerNoResult = 1;
    @Column(name = "timezone", nullable = false, length = 64) @Builder.Default private String timezone = "UTC";
    @Column(name = "preset_code", length = 64) private String presetCode;
    @Column(name = "locked_at") private Instant lockedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_robin_mode", nullable = false, length = 16)
    @Builder.Default
    private RoundRobinMode roundRobinMode = RoundRobinMode.SINGLE;

    @Column(name = "points_per_win", nullable = false)
    @Builder.Default
    private Integer pointsPerWin = 3;

    @Column(name = "points_per_loss", nullable = false)
    @Builder.Default
    private Integer pointsPerLoss = 0;

    @Column(name = "tie_breaker_priority", nullable = false, length = 500)
    @Builder.Default
    private String tieBreakerPriority = "HEAD_TO_HEAD,FORMAT_WINS,FORMAT_DIFF,MATCHES_WON,NAME";

    @Column(name = "qualifying_team_count", nullable = false)
    @Builder.Default
    private Integer qualifyingTeamCount = 4;

    @Column(name = "venues", nullable = false, columnDefinition = "text")
    @Builder.Default
    private String venues = "Center Court";

    @Column(name = "league_start_at")
    private Instant leagueStartAt;

    @Column(name = "fixture_spacing_days", nullable = false)
    @Builder.Default
    private Integer fixtureSpacingDays = 1;

    @Column(name="allowed_weekdays",nullable=false,length=100) @Builder.Default private String allowedWeekdays="MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";
    @Column(name="daily_start_time",nullable=false) @Builder.Default private LocalTime dailyStartTime=LocalTime.of(9,0);
    @Column(name="daily_end_time",nullable=false) @Builder.Default private LocalTime dailyEndTime=LocalTime.of(22,0);
    @Column(name="fixture_slot_minutes",nullable=false) @Builder.Default private Integer fixtureSlotMinutes=120;
    @Column(name="maximum_fixtures_per_day",nullable=false) @Builder.Default private Integer maximumFixturesPerDay=20;
    @Column(name="blackout_dates",nullable=false,columnDefinition="text") @Builder.Default private String blackoutDates="";
    @Column(name="minimum_rest_hours",nullable=false) @Builder.Default private Integer minimumRestHours=0;
    @Column(name="simultaneous_fixtures_allowed",nullable=false) @Builder.Default private Boolean simultaneousFixturesAllowed=true;
    @Column(name="team_may_play_twice_per_day",nullable=false) @Builder.Default private Boolean teamMayPlayTwicePerDay=false;

    @Column(name = "manual_date_editing", nullable = false)
    @Builder.Default
    private Boolean manualDateEditing = true;

    @Column(name = "result_schema_version", nullable = false)
    @Builder.Default
    private Integer resultSchemaVersion = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_team_id")
    private Team champion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "runner_up_team_id")
    private Team runnerUp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void createTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() { updatedAt = Instant.now(); }
}
