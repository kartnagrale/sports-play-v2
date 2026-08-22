package com.neml.badminton.repository;

import com.neml.badminton.entity.AuctionState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;
import java.util.List;
import java.time.Instant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionStateRepository extends JpaRepository<AuctionState, UUID> {
    Optional<AuctionState> findByAuctionIdAndChampionshipId(UUID auctionId, UUID championshipId);
    List<AuctionState> findAllByChampionshipId(UUID championshipId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuctionState s where s.auction.id = :auctionId and s.championship.id = :championshipId")
    Optional<AuctionState> findByAuctionIdForUpdate(@Param("auctionId") UUID auctionId,
                                                    @Param("championshipId") UUID championshipId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuctionState s where s.id = :id")
    Optional<AuctionState> findByIdForUpdate(@Param("id") UUID id);
    @Query("select s.id from AuctionState s where s.status = com.neml.badminton.entity.AuctionStatus.RUNNING and s.currentPlayer is not null and s.bidDeadline <= :now")
    List<UUID> findExpiredStateIds(@Param("now") Instant now);
}
