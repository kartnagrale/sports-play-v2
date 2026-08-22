package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "championships", indexes = {
        @Index(name = "idx_championship_room_code", columnList = "room_code", unique = true),
        @Index(name = "idx_championship_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Championship {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "sport_type", nullable = false, length = 64)
    private String sportType;

    @Column(name = "room_code", nullable = false, unique = true, length = 32)
    private String roomCode;

    @Column(name = "passcode_hash")
    private String passcodeHash;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    @Builder.Default
    private ChampionshipStatus status = ChampionshipStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
