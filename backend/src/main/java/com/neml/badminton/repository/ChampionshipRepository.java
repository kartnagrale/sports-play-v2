package com.neml.badminton.repository;

import com.neml.badminton.entity.Championship;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ChampionshipRepository extends JpaRepository<Championship, UUID> {
    Optional<Championship> findByRoomCodeIgnoreCase(String roomCode);
    boolean existsByRoomCodeIgnoreCase(String roomCode);
}
