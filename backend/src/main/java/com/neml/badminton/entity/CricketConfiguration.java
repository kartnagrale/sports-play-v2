package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="cricket_configurations", uniqueConstraints=@UniqueConstraint(name="uq_cricket_config_championship_version",columnNames={"championship_id","configuration_version"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CricketConfiguration {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Version @Column(name="row_version",nullable=false) private Long rowVersion;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="championship_id",nullable=false) private Championship championship;
    @Column(name="configuration_version",nullable=false) private Integer configurationVersion;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) @Builder.Default private ConfigurationStatus status=ConfigurationStatus.DRAFT;
    @Column(name="preset_code",nullable=false,length=16) @Builder.Default private String presetCode="T20";
    @Column(name="players_per_side",nullable=false) @Builder.Default private Integer playersPerSide=11;
    @Column(name="overs_per_innings",nullable=false) @Builder.Default private Integer oversPerInnings=20;
    @Column(name="maximum_overs_per_bowler",nullable=false) @Builder.Default private Integer maximumOversPerBowler=4;
    @Column(name="required_wicketkeepers",nullable=false) @Builder.Default private Integer requiredWicketkeepers=1;
    @Column(name="tie_policy",nullable=false,length=32) @Builder.Default private String tiePolicy="SUPER_OVER";
    @Column(name="maximum_super_overs",nullable=false) @Builder.Default private Integer maximumSuperOvers=1;
    @Column(name="powerplay_ranges",nullable=false,length=100) @Builder.Default private String powerplayRanges="1-6";
    @Column(name="wide_ball_runs",nullable=false) @Builder.Default private Integer wideBallRuns=1;
    @Column(name="no_ball_runs",nullable=false) @Builder.Default private Integer noBallRuns=1;
    @Column(name="free_hit_enabled",nullable=false) @Builder.Default private Boolean freeHitEnabled=true;
    @Column(name="toss_required",nullable=false) @Builder.Default private Boolean tossRequired=true;
    @Column(name="abandonment_policy",nullable=false,length=40) @Builder.Default private String abandonmentPolicy="ADMINISTRATIVE_NO_RESULT";
    @Column(name="administrative_target_override_allowed",nullable=false) @Builder.Default private Boolean administrativeTargetOverrideAllowed=true;
    @Column(name="administrative_result_override_allowed",nullable=false) @Builder.Default private Boolean administrativeResultOverrideAllowed=true;
    @Column(name="playing_xi_lock_policy",nullable=false,length=40) @Builder.Default private String playingXiLockPolicy="LOCK_ON_SUBMISSION";
    @Column(name="net_run_rate_policy",nullable=false,length=40) @Builder.Default private String netRunRatePolicy="STANDARD";
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @PrePersist void create(){if(createdAt==null)createdAt=Instant.now();}
}
