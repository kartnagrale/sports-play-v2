package com.neml.badminton.repository;

import com.neml.badminton.entity.AuctionState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface AuctionStateRepository extends JpaRepository<AuctionState, UUID> {
    Optional<AuctionState> findByAuctionId(UUID auctionId);
}
