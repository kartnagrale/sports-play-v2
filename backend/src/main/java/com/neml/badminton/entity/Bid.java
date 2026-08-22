package com.neml.badminton.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bids", uniqueConstraints = @UniqueConstraint(columnNames = {"auction_id", "sequence_no"}),
        indexes = @Index(name = "idx_bid_auction", columnList = "auction_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "sequence_no", nullable = false)
    private Long sequenceNo;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}
