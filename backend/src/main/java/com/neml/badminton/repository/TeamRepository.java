package com.neml.badminton.repository;

import com.neml.badminton.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findAllByChampionshipIdOrderByName(UUID championshipId);
    Optional<Team> findByIdAndChampionshipId(UUID id, UUID championshipId);
}
