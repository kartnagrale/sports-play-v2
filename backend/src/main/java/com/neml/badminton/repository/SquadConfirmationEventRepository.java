package com.neml.badminton.repository;

import com.neml.badminton.entity.SquadConfirmationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SquadConfirmationEventRepository extends JpaRepository<SquadConfirmationEvent, UUID> {
    List<SquadConfirmationEvent> findTop50ByChampionshipIdOrderByOccurredAtDesc(UUID championshipId);
}
