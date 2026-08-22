package com.neml.badminton.repository;

import com.neml.badminton.entity.Match;
import com.neml.badminton.entity.MatchStatus;
import com.neml.badminton.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findAllByChampionshipIdOrderByMatchNumberAsc(UUID championshipId);
    List<Match> findAllByChampionshipIdAndStatusOrderByScheduledAtAsc(UUID championshipId, MatchStatus status);
    java.util.Optional<Match> findByIdAndChampionshipId(UUID id, UUID championshipId);
    @Query("select coalesce(max(m.matchNumber), 0) from Match m where m.championship.id = :championshipId")
    int maxMatchNumber(@Param("championshipId") UUID championshipId);
}
