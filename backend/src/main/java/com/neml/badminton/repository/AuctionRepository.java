package com.neml.badminton.repository;

import com.neml.badminton.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {
    List<Auction> findAllByChampionshipIdOrderByCreatedAtDesc(UUID championshipId);
    Optional<Auction> findByIdAndChampionshipId(UUID id, UUID championshipId);
    boolean existsByIdAndChampionshipId(UUID id, UUID championshipId);
}
