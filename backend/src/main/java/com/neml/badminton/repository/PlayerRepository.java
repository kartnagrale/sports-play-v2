package com.neml.badminton.repository;

import com.neml.badminton.entity.Player;
import com.neml.badminton.entity.PlayerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
    int countByAuctionIdAndChampionshipIdAndStatus(UUID auctionId,UUID championshipId,PlayerStatus status);
    java.util.Optional<Player> findFirstByAuctionIdAndChampionshipIdAndStatusOrderByAuctionOrderAsc(UUID auctionId,UUID championshipId,PlayerStatus status);
    long countByChampionshipId(UUID championshipId);
    List<Player> findAllByChampionshipIdAndTeamIsNotNull(UUID championshipId);
    List<Player> findAllByChampionshipIdAndTeamIdOrderByAuctionOrderAsc(UUID championshipId,UUID teamId);
    List<Player> findAllByChampionshipIdOrderByAuctionOrderAsc(UUID championshipId);
    List<Player> findAllByChampionshipIdAndStatusOrderByAuctionOrderAsc(UUID championshipId, PlayerStatus status);
    List<Player> findAllByAuctionIdAndChampionshipIdAndStatusOrderByAuctionOrderAsc(UUID auctionId, UUID championshipId, PlayerStatus status);
    java.util.Optional<Player> findByIdAndChampionshipId(UUID id, UUID championshipId);
}
