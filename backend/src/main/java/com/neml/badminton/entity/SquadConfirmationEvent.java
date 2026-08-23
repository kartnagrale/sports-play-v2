package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "squad_confirmation_events", indexes = {
        @Index(name = "idx_squad_event_championship", columnList = "championship_id,occurred_at"),
        @Index(name = "idx_squad_event_team", columnList = "team_id,occurred_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SquadConfirmationEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmation_id", nullable = false)
    private SquadConfirmation confirmation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SquadConfirmationAction action;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, columnDefinition = "text")
    private String rosterSnapshot;

    @PrePersist void timestamp() { if (occurredAt == null) occurredAt = Instant.now(); }
}
