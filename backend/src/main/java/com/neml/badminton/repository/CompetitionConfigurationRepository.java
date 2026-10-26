package com.neml.badminton.repository;

import com.neml.badminton.entity.CompetitionConfiguration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CompetitionConfigurationRepository extends JpaRepository<CompetitionConfiguration, UUID> {
    Optional<CompetitionConfiguration> findByChampionshipId(UUID championshipId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select config from CompetitionConfiguration config where config.championship.id=:championshipId")
    Optional<CompetitionConfiguration> findByChampionshipIdForUpdate(@Param("championshipId") UUID championshipId);
}
