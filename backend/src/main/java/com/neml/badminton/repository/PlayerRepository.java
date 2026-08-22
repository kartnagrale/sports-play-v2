package com.neml.badminton.repository;

import com.neml.badminton.entity.Player;
import com.neml.badminton.entity.PlayerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
    List<Player> findAllByStatusOrderByAuctionOrderAsc(PlayerStatus status);
    List<Player> findAllByOrderByAuctionOrderAsc();
    List<Player> findAllByTeam(com.neml.badminton.entity.Team team);
    List<Player> findAllByChampionshipIdOrderByAuctionOrderAsc(UUID championshipId);
    List<Player> findAllByChampionshipIdAndStatusOrderByAuctionOrderAsc(UUID championshipId, PlayerStatus status);
    List<Player> findAllByAuctionIdAndStatusOrderByAuctionOrderAsc(UUID auctionId, PlayerStatus status);
    java.util.Optional<Player> findByIdAndChampionshipId(UUID id, UUID championshipId);
}
