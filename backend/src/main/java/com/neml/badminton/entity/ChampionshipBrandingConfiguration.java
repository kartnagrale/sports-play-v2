package com.neml.badminton.entity;
import jakarta.persistence.*;import lombok.*;import java.time.Instant;import java.util.UUID;
@Entity @Table(name="championship_branding_configurations",uniqueConstraints=@UniqueConstraint(name="uq_branding_config_version",columnNames={"championship_id","configuration_version"}))
@Getter@Setter@NoArgsConstructor@AllArgsConstructor@Builder
public class ChampionshipBrandingConfiguration{
 @Id@GeneratedValue(strategy=GenerationType.UUID)private UUID id;@Version@Column(name="row_version",nullable=false)private Long rowVersion;@ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="championship_id",nullable=false)private Championship championship;
 @Column(name="configuration_version",nullable=false)private Integer configurationVersion;@Enumerated(EnumType.STRING)@Column(nullable=false,length=16)@Builder.Default private ConfigurationStatus status=ConfigurationStatus.DRAFT;
 @Column(name="display_name",nullable=false,length=150)private String displayName;@Column(name="short_name",nullable=false,length=32)private String shortName;
 @Column(name="logo_reference",length=500)private String logoReference;@Column(name="primary_color",nullable=false,length=16)@Builder.Default private String primaryColor="#7CFF6B";@Column(name="secondary_color",nullable=false,length=16)@Builder.Default private String secondaryColor="#111827";
 @Column(name="sponsor_name",length=150)private String sponsorName;@Column(name="sponsor_logo_reference",length=500)private String sponsorLogoReference;@Column(name="public_description",columnDefinition="text")private String publicDescription;@Column(name="contact_information",length=500)private String contactInformation;
 @Column(name="public_visibility",nullable=false)@Builder.Default private Boolean publicVisibility=false;@Column(name="created_at",nullable=false)private Instant createdAt;@PrePersist void create(){if(createdAt==null)createdAt=Instant.now();}
}
