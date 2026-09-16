package com.neml.badminton.repository;
import com.neml.badminton.entity.CricketDelivery; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface CricketDeliveryRepository extends JpaRepository<CricketDelivery,UUID>{
 Optional<CricketDelivery> findByChampionshipIdAndIdempotencyKey(UUID cid,String key);
 Optional<CricketDelivery> findByIdAndChampionshipId(UUID id,UUID cid);
 Optional<CricketDelivery> findTopByInningsIdOrderByDeliverySequenceDesc(UUID inningsId);
 Optional<CricketDelivery> findTopByChampionshipIdAndInningsIdAndVoidedFalseOrderByDeliverySequenceDesc(UUID cid,UUID inningsId);
 List<CricketDelivery> findAllByChampionshipIdAndInningsIdAndVoidedFalseOrderByDeliverySequence(UUID cid,UUID inningsId);
 long countByChampionshipIdAndInningsIdAndBowlerPlayerIdAndLegalBallTrueAndVoidedFalse(UUID cid,UUID inningsId,UUID bowlerPlayerId);
}
