package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seasons", uniqueConstraints = @UniqueConstraint(columnNames = {"championship_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Season {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean active = false;

    @Column(nullable = false)
    private Instant startDate;

    private String description;
}
