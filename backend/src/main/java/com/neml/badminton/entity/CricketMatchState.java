package com.neml.badminton.entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="cricket_match_states",uniqueConstraints=@UniqueConstraint(name="uq_cricket_state_match_tenant",columnNames={"match_id","championship_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CricketMatchState {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Version private Long version;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="championship_id",nullable=false) private Championship championship;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="match_id",nullable=false,unique=true) private Match match;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="toss_winner_team_id") private Team tossWinner;
 @Column(name="toss_decision",length=8) private String tossDecision;
 @Column(name="target_runs") private Integer targetRuns;
 @Column(name="result_text",length=255) private String resultText;
 @Column(name="super_over_count",nullable=false) @Builder.Default private Integer superOverCount=0;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @PrePersist @PreUpdate void touch(){updatedAt=Instant.now();}
}
