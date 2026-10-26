package com.neml.badminton.entity;
import jakarta.persistence.*; import lombok.*; import java.util.UUID;
@Entity @Table(name="badminton_rubber_rules",uniqueConstraints=@UniqueConstraint(name="uq_badminton_rubber_code",columnNames={"badminton_configuration_id","rubber_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BadmintonRubberRule {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="badminton_configuration_id",nullable=false) private BadmintonConfiguration configuration;
 @Column(name="rubber_code",nullable=false,length=32) private String rubberCode; @Column(name="display_label",nullable=false,length=100) private String displayLabel;
 @Column(name="short_label",nullable=false,length=16) private String shortLabel; @Column(name="display_order",nullable=false) private Integer displayOrder;
 @Column(name="participant_count_per_side",nullable=false) private Integer participantCountPerSide; @Column(name="gender_rule",nullable=false,length=32) private String genderRule;
 @Column(name="best_of_games",nullable=false) @Builder.Default private Integer bestOfGames=3; @Column(name="points_per_game",nullable=false) @Builder.Default private Integer pointsPerGame=21;
 @Column(name="win_by",nullable=false) @Builder.Default private Integer winBy=2; @Column(name="score_cap",nullable=false) @Builder.Default private Integer scoreCap=30;
 @Column(nullable=false) @Builder.Default private Boolean enabled=true;
}
