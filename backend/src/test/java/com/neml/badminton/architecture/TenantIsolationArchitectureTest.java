package com.neml.badminton.architecture;

import com.neml.badminton.controller.*;
import com.neml.badminton.entity.AuctionState;
import com.neml.badminton.entity.Team;
import com.neml.badminton.entity.Match;
import com.neml.badminton.entity.MatchFormat;
import com.neml.badminton.websocket.AuctionBroadcaster;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIsolationArchitectureTest {

    @Test
    void championshipResourcesOnlyExposeChampionshipQualifiedControllers() {
        List<Class<?>> tenantControllers = List.of(
                TenantAuctionController.class, TenantDataController.class,
                TenantMatchController.class, TenantAnalyticsController.class,
                TenantPlayerAdminController.class);

        tenantControllers.forEach(controller -> {
            String[] roots = controller.getAnnotation(RequestMapping.class).value();
            assertThat(roots).allMatch(path -> path.contains("/{championshipId}"));
        });
    }

    @Test
    void mutableAuctionAggregatesUseOptimisticVersions() {
        assertThat(versioned(AuctionState.class)).isTrue();
        assertThat(versioned(Team.class)).isTrue();
        assertThat(versioned(Match.class)).isTrue();
        assertThat(versioned(MatchFormat.class)).isTrue();
    }

    @Test
    void broadcastsHaveNoGlobalDestinationOverloads() {
        assertThat(Arrays.stream(AuctionBroadcaster.class.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("broadcast"))
                .allMatch(method -> method.getParameterTypes()[0].equals(java.util.UUID.class))).isTrue();
    }

    private boolean versioned(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).anyMatch(field -> field.isAnnotationPresent(Version.class));
    }

}
