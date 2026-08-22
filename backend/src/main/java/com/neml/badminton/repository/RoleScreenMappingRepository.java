package com.neml.badminton.repository;

import com.neml.badminton.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface RoleScreenMappingRepository extends JpaRepository<RoleScreenMapping, UUID> {
    Optional<RoleScreenMapping> findByRoleAndScreenId(NavigationRole role, UUID screenId);

    @Query("""
            select mapping.screen from RoleScreenMapping mapping
            where mapping.role = :role and mapping.visible = true and mapping.screen.active = true
            order by mapping.screen.displayOrder asc
            """)
    List<AppScreen> findVisibleScreens(@Param("role") NavigationRole role);
}
