package com.neml.badminton.repository;

import com.neml.badminton.entity.Championship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface ChampionshipRepository extends JpaRepository<Championship, UUID> {
    Optional<Championship> findByRoomCodeIgnoreCase(String roomCode);
    boolean existsByRoomCodeIgnoreCase(String roomCode);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Championship c where c.id = :id")
    Optional<Championship> findByIdForUpdate(@Param("id") UUID id);
}
