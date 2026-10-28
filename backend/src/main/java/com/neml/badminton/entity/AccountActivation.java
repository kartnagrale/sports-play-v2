package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="account_activations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountActivation {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @Column(nullable=false,unique=true,length=64) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false) @Builder.Default private Integer credentialVersion=0;
    private Instant usedAt;
}
