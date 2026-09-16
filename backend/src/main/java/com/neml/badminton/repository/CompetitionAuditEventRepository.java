package com.neml.badminton.repository;

import com.neml.badminton.entity.CompetitionAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface CompetitionAuditEventRepository extends JpaRepository<CompetitionAuditEvent, UUID> {
    List<CompetitionAuditEvent> findTop100ByChampionshipIdOrderByOccurredAtDesc(UUID championshipId);
}
