package com.neml.badminton.repository;

import com.neml.badminton.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface SquadConfirmationRepository extends JpaRepository<SquadConfirmation, UUID> {
    List<SquadConfirmation> findAllByChampionshipId(UUID championshipId);
    Optional<SquadConfirmation> findByChampionshipIdAndTeamId(UUID championshipId, UUID teamId);
    boolean existsByChampionshipIdAndStatusIn(UUID championshipId, Collection<SquadConfirmationStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select confirmation from SquadConfirmation confirmation where confirmation.championship.id=:championshipId and confirmation.team.id=:teamId")
    Optional<SquadConfirmation> findForUpdate(@Param("championshipId") UUID championshipId, @Param("teamId") UUID teamId);
}
