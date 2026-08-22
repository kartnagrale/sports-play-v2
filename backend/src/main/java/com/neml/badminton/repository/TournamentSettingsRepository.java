package com.neml.badminton.repository;

import com.neml.badminton.entity.TournamentSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface TournamentSettingsRepository extends JpaRepository<TournamentSettings, UUID> {
    Optional<TournamentSettings> findByChampionshipId(UUID championshipId);
}
