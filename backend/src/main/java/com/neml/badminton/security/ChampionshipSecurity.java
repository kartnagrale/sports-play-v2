package com.neml.badminton.security;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.ChampionshipRoleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.*;

@Component("championshipSecurity")
public class ChampionshipSecurity {
    private final ChampionshipRoleRepository roles;

    public ChampionshipSecurity(ChampionshipRoleRepository roles) { this.roles = roles; }

    public boolean canView(UUID championshipId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        if (auth.getPrincipal() instanceof ViewerPrincipal viewer) {
            return championshipId.equals(viewer.championshipId());
        }
        if (auth.getPrincipal() instanceof User user) {
            return user.getRole() == Role.SUPER_ADMIN || roles.findByUserIdAndChampionshipId(user.getId(), championshipId).isPresent();
        }
        return false;
    }

    public boolean canManage(UUID championshipId, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User user)) return false;
        return user.getRole() == Role.SUPER_ADMIN || roles.existsByUserIdAndChampionshipIdAndRoleIn(
                user.getId(), championshipId, List.of(ChampionshipRoleType.CHAMPIONSHIP_ADMIN));
    }

    public boolean canBid(UUID championshipId, UUID teamId, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User user)) return false;
        if (user.getRole() == Role.SUPER_ADMIN) return true;
        return roles.findByUserIdAndChampionshipId(user.getId(), championshipId)
                .filter(r -> r.getRole() == ChampionshipRoleType.CHAMPIONSHIP_ADMIN ||
                        (r.getRole() == ChampionshipRoleType.TEAM_CAPTAIN && r.getTeam() != null && r.getTeam().getId().equals(teamId)))
                .isPresent();
    }
}
