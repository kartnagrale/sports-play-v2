package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "matches", uniqueConstraints = {
        @UniqueConstraint(name = "uq_matches_id_championship", columnNames = {"id", "championship_id"}),
        @UniqueConstraint(name = "uq_match_number", columnNames = {"championship_id", "match_number"}),
        @UniqueConstraint(name = "uq_match_fixture_key", columnNames = {"championship_id", "fixture_key"})},
        indexes = {
                @Index(name = "idx_match_championship", columnList = "championship_id"),
                @Index(name = "idx_match_stage", columnList = "championship_id,stage_type,status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_stage_id")
    private CompetitionStage competitionStage;

    @Column(name = "stage_championship_id")
    private UUID stageChampionshipId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type", nullable = false, length = 24)
    @Builder.Default
    private CompetitionStageType stageType = CompetitionStageType.LEAGUE;

    @Column(name = "round_number", nullable = false)
    @Builder.Default
    private Integer roundNumber = 1;

    @Column(name = "leg_number", nullable = false)
    @Builder.Default
    private Integer legNumber = 1;

    @Column(name = "fixture_key", nullable = false, length = 180)
    private String fixtureKey;

    @Column(name = "seed_a")
    private Integer seedA;

    @Column(name = "seed_b")
    private Integer seedB;

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

    @Column(name = "team_a_points", nullable = false)
    @Builder.Default
    private Integer teamAPoints = 0;

    @Column(name = "team_b_points", nullable = false)
    @Builder.Default
    private Integer teamBPoints = 0;

    @Column(name = "result_revision", nullable = false)
    @Builder.Default
    private Integer resultRevision = 0;

    /** Sport-defined score/period/set data, kept as JSON text for portability across PostgreSQL and H2. */
    @Column(name = "score_data", columnDefinition = "text")
    @Builder.Default
    private String scoreData = "{}";
}
