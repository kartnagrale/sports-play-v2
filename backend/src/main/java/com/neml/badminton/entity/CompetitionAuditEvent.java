package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "competition_audit_events",
        indexes = @Index(name = "idx_competition_audit_championship", columnList = "championship_id,occurred_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompetitionAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id")
    private Match fixture;

    @Column(name = "fixture_championship_id")
    private UUID fixtureChampionshipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false)
    @Builder.Default
    private Integer revision = 1;
    @Column(name="configuration_version") private Integer configurationVersion;
    @Column(name="entity_type",nullable=false,length=32) private String entityType;
    @Column(name="entity_id",nullable=false) private UUID entityId;
    @Column(length=500) private String reason;
    @Column(name="correlation_id",nullable=false) private UUID correlationId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist void timestamp() { if (occurredAt == null) occurredAt = Instant.now(); }
}
