package com.neml.badminton.entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="cricket_deliveries",uniqueConstraints={@UniqueConstraint(name="uq_cricket_delivery_idempotency",columnNames={"championship_id","idempotency_key"}),@UniqueConstraint(name="uq_cricket_delivery_sequence",columnNames={"innings_id","delivery_sequence"})})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CricketDelivery {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="championship_id",nullable=false) private Championship championship;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="innings_id",nullable=false) private CricketInnings innings;
 @Column(name="delivery_sequence",nullable=false) private Integer deliverySequence;
 @Column(name="striker_player_id",nullable=false) private UUID strikerPlayerId; @Column(name="non_striker_player_id",nullable=false) private UUID nonStrikerPlayerId;
 @Column(name="bowler_player_id",nullable=false) private UUID bowlerPlayerId; @Column(name="wicket_player_id") private UUID wicketPlayerId;
 @Column(name="idempotency_key",nullable=false,length=100) private String idempotencyKey;
 @Column(name="batter_runs",nullable=false) private Integer batterRuns; @Column(nullable=false) private Integer wides;
 @Column(name="no_balls",nullable=false) private Integer noBalls; @Column(nullable=false) private Integer byes;
 @Column(name="leg_byes",nullable=false) private Integer legByes; @Column(name="penalty_runs",nullable=false) private Integer penaltyRuns;
 @Column(nullable=false) private Boolean wicket; @Column(name="dismissal_type",length=32) private String dismissalType;
 @Column(name="legal_ball",nullable=false) private Boolean legalBall; @Column(name="total_runs",nullable=false) private Integer totalRuns;
 @Column(name="occurred_at",nullable=false) private Instant occurredAt;
 @Column(nullable=false) @Builder.Default private Boolean voided=false;
 @Column(name="correction_reason",length=500) private String correctionReason;
 @Column(name="corrected_at") private Instant correctedAt;
 @Column(name="corrected_by_user_id") private UUID correctedByUserId;
}
