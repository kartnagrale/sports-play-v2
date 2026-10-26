package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "competition_stages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_competition_stage_id_tenant", columnNames = {"id", "championship_id"}),
                @UniqueConstraint(name = "uq_competition_stage_type", columnNames = {"championship_id", "stage_type"})
        },
        indexes = @Index(name = "idx_competition_stage_championship", columnList = "championship_id,stage_order"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionStage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type", nullable = false, length = 24)
    private CompetitionStageType stageType;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    @Builder.Default
    private CompetitionStageStatus status = CompetitionStageStatus.NOT_STARTED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "standings_snapshot", columnDefinition = "text")
    private String standingsSnapshot;

    @Column(name = "stage_metadata", nullable = false, columnDefinition = "text")
    @Builder.Default
    private String stageMetadata = "{}";
}
