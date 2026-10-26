package com.neml.badminton.repository;

import com.neml.badminton.entity.AppScreen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AppScreenRepository extends JpaRepository<AppScreen, UUID> {
    Optional<AppScreen> findByCode(String code);
}
