package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "matches", indexes = @Index(name = "idx_match_championship", columnList = "championship_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "team_a_id", nullable = false)
    private Team teamA;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "team_b_id", nullable = false)
    private Team teamB;

    @Column(nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private Team winnerTeam;

    @Column(name = "team_a_format_wins", nullable = false)
    private Integer teamAFormatWins = 0;

    @Column(name = "team_b_format_wins", nullable = false)
    private Integer teamBFormatWins = 0;

    @Column(nullable = false)
    private Integer matchNumber;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MatchFormat> formats = new ArrayList<>();

    private String venue;

    /** Sport-defined score/period/set data, kept as JSON text for portability across PostgreSQL and H2. */
    @Column(name = "score_data", columnDefinition = "text")
    @Builder.Default
    private String scoreData = "{}";
}
