package com.neml.badminton.repository;

import com.neml.badminton.entity.CricketConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface CricketConfigurationRepository extends JpaRepository<CricketConfiguration, UUID> {
    Optional<CricketConfiguration> findFirstByChampionshipIdOrderByConfigurationVersionDesc(UUID championshipId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CricketConfiguration c where c.championship.id=:cid and c.configurationVersion=(select max(x.configurationVersion) from CricketConfiguration x where x.championship.id=:cid)")
    Optional<CricketConfiguration> findLatestForUpdate(@Param("cid") UUID championshipId);
}
