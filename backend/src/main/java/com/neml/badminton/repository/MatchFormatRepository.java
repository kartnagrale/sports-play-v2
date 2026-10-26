package com.neml.badminton.repository;

import com.neml.badminton.entity.MatchFormat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface MatchFormatRepository extends JpaRepository<MatchFormat, UUID> {
    Optional<MatchFormat> findByIdAndMatchChampionshipId(UUID id, UUID championshipId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select format from MatchFormat format where format.id=:id and format.match.championship.id=:championshipId")
    Optional<MatchFormat> findByIdAndChampionshipIdForUpdate(@Param("id") UUID id,
                                                             @Param("championshipId") UUID championshipId);
}
