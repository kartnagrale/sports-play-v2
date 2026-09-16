package com.neml.badminton.entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="cricket_playing_members",uniqueConstraints={@UniqueConstraint(name="uq_cricket_xi_player",columnNames={"championship_id","match_id","team_id","player_id"})})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CricketPlayingMember {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="championship_id",nullable=false) private Championship championship;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="match_id",nullable=false) private Match match;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="team_id",nullable=false) private Team team;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="player_id",nullable=false) private Player player;
 @Column(nullable=false) private Boolean captain; @Column(nullable=false) private Boolean wicketkeeper;
 @Column(name="locked_at",nullable=false) private Instant lockedAt;
}
