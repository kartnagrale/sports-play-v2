package com.neml.badminton.entity;
import jakarta.persistence.*;import lombok.*;import java.util.UUID;
@Entity @Table(name="roster_quota_rules",uniqueConstraints=@UniqueConstraint(name="uq_roster_quota_rule",columnNames={"championship_id","configuration_version","rule_code"}))
@Getter@Setter@NoArgsConstructor@AllArgsConstructor@Builder
public class RosterQuotaRule{
 @Id@GeneratedValue(strategy=GenerationType.UUID)private UUID id;@Version@Column(name="row_version",nullable=false)private Long rowVersion;@ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="championship_id",nullable=false)private Championship championship;
 @Column(name="configuration_version",nullable=false)@Builder.Default private Integer configurationVersion=1;@Enumerated(EnumType.STRING)@Column(nullable=false,length=16)@Builder.Default private ConfigurationStatus status=ConfigurationStatus.VALID;
 @Column(name="sport_code",nullable=false,length=32)private String sportCode;@Column(name="rule_code",nullable=false,length=64)private String ruleCode;
 @Column(name="category_type",nullable=false,length=32)private String categoryType;@Column(name="category_value",nullable=false,length=64)private String categoryValue;
 @Column(name="minimum_count")private Integer minimumCount;@Column(name="maximum_count")private Integer maximumCount;@Column(name="applies_to",nullable=false,length=32)private String appliesTo;
 @Column(nullable=false)@Builder.Default private Boolean enabled=true;@Column(name="validation_message",length=255)private String validationMessage;
}
