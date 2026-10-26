package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "competition_qualifications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_competition_qualification_seed", columnNames = {"championship_id", "source_stage", "seed"}),
                @UniqueConstraint(name = "uq_competition_qualification_team", columnNames = {"championship_id", "source_stage", "team_id"})
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionQualification {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_stage", nullable = false, length = 24)
    private CompetitionStageType sourceStage;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private Integer seed;

    @Column(name = "qualified_at", nullable = false)
    private Instant qualifiedAt;

    @PrePersist void timestamp() { if (qualifiedAt == null) qualifiedAt = Instant.now(); }
}
