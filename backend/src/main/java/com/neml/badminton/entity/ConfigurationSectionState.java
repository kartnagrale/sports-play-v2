package com.neml.badminton.entity;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="configuration_section_states",uniqueConstraints=@UniqueConstraint(name="uq_configuration_section",columnNames={"championship_id","section_code"}))
@Getter@Setter@NoArgsConstructor@AllArgsConstructor@Builder
public class ConfigurationSectionState{
 @Id@GeneratedValue(strategy=GenerationType.UUID)private UUID id;@Version private Long rowVersion;
 @ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="championship_id",nullable=false)private Championship championship;
 @Column(name="section_code",nullable=false,length=40)private String sectionCode;@Column(name="configuration_version",nullable=false)@Builder.Default private Integer configurationVersion=1;
 @Enumerated(EnumType.STRING)@Column(nullable=false,length=16)@Builder.Default private ConfigurationStatus status=ConfigurationStatus.DRAFT;
 @Column(name="locked_at")private Instant lockedAt;@ManyToOne(fetch=FetchType.LAZY)@JoinColumn(name="last_changed_by")private User lastChangedBy;
 @Column(name="validation_messages",nullable=false,columnDefinition="text")@Builder.Default private String validationMessages="[]";
 @Column(name="updated_at",nullable=false)private Instant updatedAt;@PrePersist@PreUpdate void touch(){updatedAt=Instant.now();}
}
