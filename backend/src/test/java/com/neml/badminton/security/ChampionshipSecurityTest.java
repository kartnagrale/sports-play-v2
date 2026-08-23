package com.neml.badminton.security;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.ChampionshipRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChampionshipSecurityTest {
    private final ChampionshipRoleRepository roles = mock(ChampionshipRoleRepository.class);
    private final ChampionshipSecurity security = new ChampionshipSecurity(roles);

    @Test
    void superAdminCanCreateAndObserveButCannotConfigureOrBid() {
        UUID championshipId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).role(Role.SUPER_ADMIN).build();
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());

        assertThat(security.canView(championshipId, auth)).isTrue();
        assertThat(security.canManage(championshipId, auth)).isFalse();
        assertThat(security.canBid(championshipId, UUID.randomUUID(), auth)).isFalse();
    }

    @Test
    void captainCanOnlyBidForAssignedTeamInsideAssignedChampionship() {
        UUID championshipId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).role(Role.USER).build();
        Team team = Team.builder().id(teamId).build();
        ChampionshipRole role = ChampionshipRole.builder().user(user).team(team)
                .role(ChampionshipRoleType.TEAM_CAPTAIN).build();
        when(roles.findByUserIdAndChampionshipId(user.getId(), championshipId)).thenReturn(Optional.of(role));
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of());

        assertThat(security.canBid(championshipId, teamId, auth)).isTrue();
        assertThat(security.canConfirmSquad(championshipId, teamId, auth)).isTrue();
        assertThat(security.canBid(championshipId, UUID.randomUUID(), auth)).isFalse();
        assertThat(security.canConfirmSquad(championshipId, UUID.randomUUID(), auth)).isFalse();
        assertThat(security.canBid(UUID.randomUUID(), teamId, auth)).isFalse();
    }
}
