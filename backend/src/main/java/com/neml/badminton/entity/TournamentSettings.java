package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "tournament_settings", uniqueConstraints = @UniqueConstraint(columnNames = "championship_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false, unique = true)
    private Championship championship;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxSquadSize = 12;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal purseLimit = new BigDecimal("1000000000");

    @Column(nullable = false)
    @Builder.Default
    private Integer minMale = 9;

    @Column(nullable = false)
    @Builder.Default
    private Integer minFemale = 3;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal playerBasePrice = new BigDecimal("2000000");

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal bidIncrement = new BigDecimal("500000");

    @Column(nullable = false)
    @Builder.Default
    private Integer timerSeconds = 30;

    @Column(name = "custom_rules", columnDefinition = "text")
    @Builder.Default
    private String customRules = "{}";

    @Column(nullable = false)
    @Builder.Default
    private Integer pointsPerWin = 3;

    @Column(nullable = false)
    @Builder.Default
    private Integer penaltyPerUnplayed = 2;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "tie_breaker_order",
            joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "tie_breaker", length = 32)
    @OrderColumn(name = "position")
    @Builder.Default
    private List<TieBreaker> tieBreakerOrder = new ArrayList<>();
}
