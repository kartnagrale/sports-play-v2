package com.neml.badminton.repository;

import com.neml.badminton.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface CompetitionStageRepository extends JpaRepository<CompetitionStage, UUID> {
    List<CompetitionStage> findAllByChampionshipIdOrderByStageOrder(UUID championshipId);
    Optional<CompetitionStage> findByChampionshipIdAndStageType(UUID championshipId, CompetitionStageType stageType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stage from CompetitionStage stage where stage.championship.id=:championshipId and stage.stageType=:stageType")
    Optional<CompetitionStage> findForUpdate(@Param("championshipId") UUID championshipId,
                                             @Param("stageType") CompetitionStageType stageType);
}
