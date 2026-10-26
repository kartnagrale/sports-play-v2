package com.neml.badminton.entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="cricket_innings",uniqueConstraints=@UniqueConstraint(name="uq_cricket_innings_number",columnNames={"championship_id","match_id","innings_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CricketInnings {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Version private Long version;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="championship_id",nullable=false) private Championship championship;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="match_id",nullable=false) private Match match;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="batting_team_id",nullable=false) private Team battingTeam;
 @Column(name="innings_number",nullable=false) private Integer inningsNumber;
 @Column(nullable=false) @Builder.Default private Integer runs=0; @Column(nullable=false) @Builder.Default private Integer wickets=0;
 @Column(name="legal_balls",nullable=false) @Builder.Default private Integer legalBalls=0;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) @Builder.Default private CricketInningsStatus status=CricketInningsStatus.LIVE;
 @Column(name="started_at",nullable=false) private Instant startedAt; @Column(name="completed_at") private Instant completedAt;
}
