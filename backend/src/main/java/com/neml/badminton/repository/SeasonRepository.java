package com.neml.badminton.repository;

import com.neml.badminton.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeasonRepository extends JpaRepository<Season, UUID> {
    Optional<Season> findFirstByChampionshipIdAndActiveTrueOrderByStartDateDesc(UUID championshipId);
    List<Season> findAllByChampionshipIdOrderByStartDateDesc(UUID championshipId);
}
