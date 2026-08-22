package com.neml.badminton.service;

import com.neml.badminton.dto.NavigationDtos.*;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.security.ViewerPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class NavigationService {
    private final RoleScreenMappingRepository mappings;
    private final ChampionshipRoleRepository championshipRoles;

    public NavigationService(RoleScreenMappingRepository mappings, ChampionshipRoleRepository championshipRoles) {
        this.mappings = mappings;
        this.championshipRoles = championshipRoles;
    }

    public NavigationResponse navigation(UUID championshipId, Authentication authentication) {
        NavigationRole role = effectiveRole(championshipId, authentication);
        List<ScreenDto> screens = mappings.findVisibleScreens(role).stream().map(ScreenDto::from).toList();
        return new NavigationResponse(role, screens);
    }

    private NavigationRole effectiveRole(UUID championshipId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (authentication.getPrincipal() instanceof ViewerPrincipal viewer) {
            if (championshipId != null && !championshipId.equals(viewer.championshipId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Viewer token is not valid for this championship");
            }
            return NavigationRole.SPECTATOR;
        }
        if (authentication.getPrincipal() instanceof User user) {
            if (user.getRole() == Role.SUPER_ADMIN) return NavigationRole.SUPER_ADMIN;
            if (championshipId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "championshipId is required for this account");
            }
            ChampionshipRoleType role = championshipRoles.findByUserIdAndChampionshipId(user.getId(), championshipId)
                    .map(ChampionshipRole::getRole)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "You do not have access to this championship"));
            return NavigationRole.valueOf(role.name());
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported account type");
    }
}
