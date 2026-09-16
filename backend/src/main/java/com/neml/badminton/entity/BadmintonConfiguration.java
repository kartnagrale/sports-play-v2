package com.neml.badminton.entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant; import java.util.*;
@Entity @Table(name="badminton_configurations",uniqueConstraints=@UniqueConstraint(name="uq_badminton_config_version",columnNames={"championship_id","configuration_version"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BadmintonConfiguration {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Version @Column(name="row_version",nullable=false) private Long rowVersion;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="championship_id",nullable=false) private Championship championship;
 @Column(name="configuration_version",nullable=false) private Integer configurationVersion;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) @Builder.Default private ConfigurationStatus status=ConfigurationStatus.DRAFT;
 @Column(name="preset_code",nullable=false,length=64) @Builder.Default private String presetCode="BADMINTON_STANDARD_FIVE";
 @Column(name="maximum_appearances_per_player",nullable=false) @Builder.Default private Integer maximumAppearancesPerPlayer=2;
 @Column(name="multiple_rubber_categories_allowed",nullable=false) @Builder.Default private Boolean multipleRubberCategoriesAllowed=false;
 @Column(name="lineup_changes_after_start_allowed",nullable=false) @Builder.Default private Boolean lineupChangesAfterStartAllowed=false;
 @Column(name="tie_handling",nullable=false,length=32) @Builder.Default private String tieHandling="LEAGUE_TIE";
 @Column(name="walkover_policy",nullable=false,length=32) @Builder.Default private String walkoverPolicy="ADMINISTRATIVE";
 @Column(name="retirement_policy",nullable=false,length=32) @Builder.Default private String retirementPolicy="OPPONENT_WIN";
 @Column(name="lineup_visibility_policy",nullable=false,length=32) @Builder.Default private String lineupVisibilityPolicy="AFTER_BOTH_SUBMIT";
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @OneToMany(mappedBy="configuration",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("displayOrder asc") @Builder.Default private List<BadmintonRubberRule> rubbers=new ArrayList<>();
 @PrePersist void create(){if(createdAt==null)createdAt=Instant.now();}
}
