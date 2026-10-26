package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "role_screen_mappings",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_screen", columnNames = {"role_key", "screen_id"}),
        indexes = @Index(name = "idx_role_screen_role", columnList = "role_key"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleScreenMapping {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_key", nullable = false, length = 40)
    private NavigationRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false)
    private AppScreen screen;

    @Column(nullable = false)
    private Boolean visible;
}
