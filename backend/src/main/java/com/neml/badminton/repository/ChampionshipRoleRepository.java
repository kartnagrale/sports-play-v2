package com.neml.badminton.repository;

import com.neml.badminton.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ChampionshipRoleRepository extends JpaRepository<ChampionshipRole, UUID> {
    List<ChampionshipRole> findAllByUserId(UUID userId);
    Optional<ChampionshipRole> findByUserIdAndChampionshipId(UUID userId, UUID championshipId);
    boolean existsByUserIdAndChampionshipIdAndRoleIn(UUID userId, UUID championshipId, Collection<ChampionshipRoleType> roles);
}
