package com.neml.badminton.seed;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Essential reference data; existing labels, visibility and custom mappings are preserved. */
@Component @Order(0)
public class ReferenceDataInitializer implements CommandLineRunner {
    private final AppScreenRepository screens;
    private final RoleScreenMappingRepository screenMappings;
    public ReferenceDataInitializer(AppScreenRepository screens,RoleScreenMappingRepository mappings){this.screens=screens;this.screenMappings=mappings;}
    @Override @Transactional public void run(String... args){seedNavigation();}
    private void seedNavigation() {
        List<ScreenSeed> catalog = List.of(
                new ScreenSeed("DASHBOARD", "Dashboard", "/dashboard", "LayoutDashboard", "Workspace", 10),
                new ScreenSeed("AUCTION", "Auction", "/auction", "Gavel", "Workspace", 20),
                new ScreenSeed("SCOREBOARD", "Scoreboard", "/scoreboard", "Trophy", "Workspace", 30),
                new ScreenSeed("SQUAD_CONFIRMATION", "Squad Confirmation", "/squad-confirmation", "ClipboardCheck", "Competition", 35),
                new ScreenSeed("COMPETITION", "Competition", "/competition", "Brackets", "Competition", 36),
                new ScreenSeed("MATCHES", "Matches", "/matches", "CalendarDays", "Workspace", 40),
                new ScreenSeed("CHAMPIONSHIP_SETTINGS", "Championship Setup", "/admin/championship-settings", "Settings2", "Management", 45),
                new ScreenSeed("TEAMS", "Teams", "/teams", "UsersRound", "Management", 50),
                new ScreenSeed("TEAM_MANAGEMENT", "Teams & Captains", "/admin/team-management", "UserCog", "Management", 55),
                new ScreenSeed("PLAYERS", "Players", "/players", "UserRound", "Management", 60),
                new ScreenSeed("ANNOUNCEMENTS", "Announcements", "/announcements", "Bell", "Management", 70),
                new ScreenSeed("ANALYSIS", "Team Analysis", "/analysis", "ChartNoAxesCombined", "Insights", 80),
                new ScreenSeed("FORMAT_LEADERS", "Format Leaders", "/format-leaders", "Medal", "Insights", 90),
                new ScreenSeed("TOP_PERFORMERS", "Top Performers", "/top-performers", "Sparkles", "Insights", 100),
                new ScreenSeed("HISTORY", "Auction History", "/history", "History", "Insights", 110),
                new ScreenSeed("CHAMPIONSHIP_LAUNCHPAD", "Launch Championship", "/admin/championship-launchpad", "Rocket", "Platform", 115),
                new ScreenSeed("CHAMPIONSHIPS", "Championships", "/admin/championships", "ShieldCheck", "Platform", 120)
        );
        Map<String, AppScreen> persisted = new HashMap<>();
        for (ScreenSeed seed : catalog) {
            AppScreen screen = screens.findByCode(seed.code()).orElseGet(() -> screens.save(AppScreen.builder()
                    .code(seed.code()).label(seed.label()).path(seed.path()).icon(seed.icon())
                    .section(seed.section()).displayOrder(seed.order()).active(true).build()));
            persisted.put(seed.code(), screen);
        }
        Map<NavigationRole, Set<String>> grants = Map.of(
                NavigationRole.SUPER_ADMIN, Set.of("CHAMPIONSHIP_LAUNCHPAD", "CHAMPIONSHIPS"),
                NavigationRole.CHAMPIONSHIP_ADMIN, Set.of("DASHBOARD", "COMPETITION", "AUCTION", "SCOREBOARD", "SQUAD_CONFIRMATION", "MATCHES", "CHAMPIONSHIP_SETTINGS", "TEAMS", "TEAM_MANAGEMENT", "PLAYERS", "ANNOUNCEMENTS", "ANALYSIS", "FORMAT_LEADERS", "TOP_PERFORMERS", "HISTORY"),
                NavigationRole.TEAM_CAPTAIN, Set.of("DASHBOARD", "COMPETITION", "AUCTION", "SCOREBOARD", "SQUAD_CONFIRMATION", "MATCHES", "TEAMS", "ANALYSIS", "FORMAT_LEADERS", "TOP_PERFORMERS", "HISTORY"),
                NavigationRole.SPECTATOR, Set.of("DASHBOARD", "COMPETITION", "AUCTION", "SCOREBOARD", "MATCHES", "TEAMS", "ANNOUNCEMENTS", "ANALYSIS", "FORMAT_LEADERS", "TOP_PERFORMERS")
        );
        grants.forEach((role, codes) -> codes.forEach(code -> {
            AppScreen screen = persisted.get(code);
            screenMappings.findByRoleAndScreenId(role, screen.getId()).orElseGet(() -> screenMappings.save(
                    RoleScreenMapping.builder().role(role).screen(screen).visible(true).build()));
        }));
    }

    private record ScreenSeed(String code, String label, String path, String icon, String section, int order) {}

}
