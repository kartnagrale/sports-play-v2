package com.neml.badminton.repository;

import com.neml.badminton.entity.Bid;
import com.neml.badminton.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, UUID> {
    List<Bid> findAllByPlayerAndChampionshipIdOrderByCreatedAtDesc(Player player, UUID championshipId);
    List<Bid> findTop50ByAuctionIdAndChampionshipIdOrderBySequenceNoDesc(UUID auctionId, UUID championshipId);
    Optional<Bid> findFirstByAuctionIdAndChampionshipIdAndPlayerAndActiveTrueOrderBySequenceNoDesc(UUID auctionId, UUID championshipId, Player player);
    @Query("select coalesce(max(b.sequenceNo), 0) from Bid b where b.auction.id = :auctionId and b.championship.id = :championshipId")
    long maxSequence(@Param("auctionId") UUID auctionId, @Param("championshipId") UUID championshipId);
}
