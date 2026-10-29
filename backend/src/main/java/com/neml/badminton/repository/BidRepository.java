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
    boolean existsByChampionshipId(UUID championshipId);
    List<Bid> findAllByPlayerAndChampionshipIdOrderByCreatedAtDesc(Player player, UUID championshipId);
    List<Bid> findTop50ByAuctionIdAndChampionshipIdOrderBySequenceNoDesc(UUID auctionId, UUID championshipId);
    Optional<Bid> findFirstByAuctionIdAndChampionshipIdAndPlayerAndActiveTrueOrderBySequenceNoDesc(UUID auctionId, UUID championshipId, Player player);
    // One highest ACTIVE bid per team for this player, independent of the history window.
    @Query("select b from Bid b where b.auction.id = :auctionId and b.championship.id = :championshipId " +
           "and b.player = :player and b.active = true and not exists (select newer.id from Bid newer " +
           "where newer.auction = b.auction and newer.championship = b.championship and newer.player = b.player " +
           "and newer.team = b.team and newer.active = true and (newer.amount > b.amount " +
           "or (newer.amount = b.amount and newer.sequenceNo > b.sequenceNo))) order by b.amount desc, b.sequenceNo asc")
    List<Bid> findCurrentTeamBids(@Param("auctionId") UUID auctionId, @Param("championshipId") UUID championshipId, @Param("player") Player player);
    @Query("select coalesce(max(b.sequenceNo), 0) from Bid b where b.auction.id = :auctionId and b.championship.id = :championshipId")
    long maxSequence(@Param("auctionId") UUID auctionId, @Param("championshipId") UUID championshipId);
}
