package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "teams", uniqueConstraints = @UniqueConstraint(columnNames = {"championship_id", "name"}),
        indexes = @Index(name = "idx_team_championship", columnList = "championship_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String shortCode;

    private String logoUrl;

    private String primaryColor;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal purseTotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal purseRemaining;

    @Column(nullable = false)
    private Integer maleCount = 0;

    @Column(nullable = false)
    private Integer femaleCount = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer matchPoints = 0;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Player> players = new ArrayList<>();
}
