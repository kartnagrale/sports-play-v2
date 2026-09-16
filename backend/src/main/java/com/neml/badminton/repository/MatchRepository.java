package com.neml.badminton.repository;

import com.neml.badminton.entity.Match;
import com.neml.badminton.entity.MatchStatus;
import com.neml.badminton.entity.CompetitionStageType;
import com.neml.badminton.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findAllByChampionshipIdOrderByMatchNumberAsc(UUID championshipId);
    List<Match> findAllByChampionshipIdAndStatusOrderByScheduledAtAsc(UUID championshipId, MatchStatus status);
    List<Match> findAllByChampionshipIdAndStageTypeOrderByMatchNumberAsc(UUID championshipId, CompetitionStageType stageType);
    List<Match> findAllByChampionshipIdAndStageTypeAndStatusOrderByMatchNumberAsc(
            UUID championshipId, CompetitionStageType stageType, MatchStatus status);
    java.util.Optional<Match> findByIdAndChampionshipId(UUID id, UUID championshipId);
    java.util.Optional<Match> findByChampionshipIdAndFixtureKey(UUID championshipId, String fixtureKey);
    boolean existsByChampionshipIdAndStageType(UUID championshipId, CompetitionStageType stageType);
    long countByChampionshipIdAndStageType(UUID championshipId, CompetitionStageType stageType);
    long countByChampionshipIdAndStageTypeAndStatus(UUID championshipId, CompetitionStageType stageType, MatchStatus status);
    @Query("""
            select (count(m) > 0) from Match m
            where m.championship.id=:championshipId and m.stageType=:stageType
              and ((m.teamA.id=:teamAId and m.teamB.id=:teamBId)
                or (m.teamA.id=:teamBId and m.teamB.id=:teamAId))
            """)
    boolean existsPair(@Param("championshipId") UUID championshipId,
                       @Param("stageType") CompetitionStageType stageType,
                       @Param("teamAId") UUID teamAId, @Param("teamBId") UUID teamBId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.id=:id and m.championship.id=:championshipId")
    java.util.Optional<Match> findByIdAndChampionshipIdForUpdate(@Param("id") UUID id,
                                                                 @Param("championshipId") UUID championshipId);
    @Query("select coalesce(max(m.matchNumber), 0) from Match m where m.championship.id = :championshipId")
    int maxMatchNumber(@Param("championshipId") UUID championshipId);
}
