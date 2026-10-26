package com.neml.badminton.repository;
import com.neml.badminton.entity.CricketMatchState; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface CricketMatchStateRepository extends JpaRepository<CricketMatchState,UUID>{
 long countByChampionshipId(UUID championshipId);
 Optional<CricketMatchState> findByMatchIdAndChampionshipId(UUID matchId,UUID championshipId);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select s from CricketMatchState s where s.match.id=:matchId and s.championship.id=:cid")
 Optional<CricketMatchState> findForUpdate(@Param("cid")UUID cid,@Param("matchId")UUID matchId);
}
