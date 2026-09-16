package com.neml.badminton.repository;

import com.neml.badminton.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface CompetitionQualificationRepository extends JpaRepository<CompetitionQualification, UUID> {
    List<CompetitionQualification> findAllByChampionshipIdAndSourceStageOrderBySeed(
            UUID championshipId, CompetitionStageType sourceStage);
    boolean existsByChampionshipIdAndSourceStage(UUID championshipId, CompetitionStageType sourceStage);
}
