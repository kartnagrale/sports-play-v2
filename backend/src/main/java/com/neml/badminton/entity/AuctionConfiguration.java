package com.neml.badminton.entity;
import jakarta.persistence.*;import lombok.*;import java.math.BigDecimal;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="auction_configurations",uniqueConstraints=@UniqueConstraint(name="uq_auction_config_version",columnNames={"championship_id","configuration_version"}))
@Getter@Setter@NoArgsConstructor@AllArgsConstructor@Builder
public class AuctionConfiguration{
 @Id@GeneratedValue(strategy=GenerationType.UUID)private UUID id;@Version@Column(name="row_version",nullable=false)private Long rowVersion;@ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="championship_id",nullable=false)private Championship championship;
 @Column(name="configuration_version",nullable=false)private Integer configurationVersion;@Enumerated(EnumType.STRING)@Column(nullable=false,length=16)@Builder.Default private ConfigurationStatus status=ConfigurationStatus.DRAFT;
 @Column(nullable=false)@Builder.Default private Boolean enabled=true;@Column(name="auction_type",nullable=false,length=32)@Builder.Default private String auctionType="OPEN_ASCENDING";
 @Column(name="team_budget",nullable=false,precision=19,scale=2)private BigDecimal teamBudget;@Column(name="minimum_bid",nullable=false,precision=19,scale=2)private BigDecimal minimumBid;@Column(name="bid_increment",nullable=false,precision=19,scale=2)private BigDecimal bidIncrement;
 @Column(name="captain_preassignment_required",nullable=false)@Builder.Default private Boolean captainPreassignmentRequired=false;
 @Column(name="maximum_bid",precision=19,scale=2)private BigDecimal maximumBid;
 @Column(name="maximum_premium_purchases")private Integer maximumPremiumPurchases;
 @Column(name="minimum_roster_size",nullable=false)private Integer minimumRosterSize;@Column(name="maximum_roster_size",nullable=false)private Integer maximumRosterSize;
 @Column(name="retained_players_allowed",nullable=false)@Builder.Default private Boolean retainedPlayersAllowed=false;@Column(name="maximum_retained_players",nullable=false)@Builder.Default private Integer maximumRetainedPlayers=0;
 @Column(name="unsold_player_reauction_allowed",nullable=false)@Builder.Default private Boolean unsoldPlayerReauctionAllowed=true;@Column(name="purse_carry_forward_policy",nullable=false,length=32)@Builder.Default private String purseCarryForwardPolicy="NONE";
 @Column(name="auction_order_policy",nullable=false,length=32)@Builder.Default private String auctionOrderPolicy="REGISTRATION_ORDER";@Column(name="timer_seconds",nullable=false)@Builder.Default private Integer timerSeconds=30;
 @Column(name="created_at",nullable=false)private Instant createdAt;@PrePersist void create(){if(createdAt==null)createdAt=Instant.now();}
}
