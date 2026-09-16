package com.neml.badminton.repository;
import com.neml.badminton.entity.CricketPlayingMember; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface CricketPlayingMemberRepository extends JpaRepository<CricketPlayingMember,UUID>{
 List<CricketPlayingMember> findAllByChampionshipIdAndMatchIdAndTeamId(UUID cid,UUID matchId,UUID teamId);
 long countByChampionshipIdAndMatchId(UUID cid,UUID matchId);
 boolean existsByChampionshipIdAndMatchIdAndTeamIdAndPlayerId(UUID cid,UUID matchId,UUID teamId,UUID playerId);
}
