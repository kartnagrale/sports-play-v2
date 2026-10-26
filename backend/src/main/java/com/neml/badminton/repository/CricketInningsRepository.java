package com.neml.badminton.repository;
import com.neml.badminton.entity.CricketInnings; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface CricketInningsRepository extends JpaRepository<CricketInnings,UUID>{
 List<CricketInnings> findAllByChampionshipIdAndMatchIdOrderByInningsNumber(UUID cid,UUID matchId);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select i from CricketInnings i where i.id=:id and i.championship.id=:cid")
 Optional<CricketInnings> findForUpdate(@Param("cid")UUID cid,@Param("id")UUID id);
}
