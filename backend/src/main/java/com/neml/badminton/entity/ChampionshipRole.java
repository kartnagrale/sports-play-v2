package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "championship_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "championship_id"}),
        indexes = {
                @Index(name = "idx_champ_role_user", columnList = "user_id"),
                @Index(name = "idx_champ_role_championship", columnList = "championship_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChampionshipRole {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChampionshipRoleType role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}
