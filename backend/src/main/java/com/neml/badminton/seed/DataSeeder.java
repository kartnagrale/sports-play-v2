package com.neml.badminton.seed;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users; private final ChampionshipRepository championships;
    private final ChampionshipRoleRepository roles; private final TournamentSettingsRepository settings;
    private final AuctionRepository auctions; private final AuctionStateRepository states;
    private final TeamRepository teams; private final PlayerRepository players; private final PasswordEncoder encoder;
    private final AppScreenRepository screens; private final RoleScreenMappingRepository screenMappings;

    public DataSeeder(UserRepository users,ChampionshipRepository championships,ChampionshipRoleRepository roles,
            TournamentSettingsRepository settings,AuctionRepository auctions,AuctionStateRepository states,
            TeamRepository teams,PlayerRepository players,PasswordEncoder encoder, AppScreenRepository screens,
            RoleScreenMappingRepository screenMappings){this.users=users;this.championships=championships;
        this.roles=roles;this.settings=settings;this.auctions=auctions;this.states=states;this.teams=teams;this.players=players;this.encoder=encoder;
        this.screens=screens;this.screenMappings=screenMappings;}

    @Override @Transactional public void run(String... args){
        User admin=users.findByEmail("admin@sports.local").orElseGet(()->users.save(User.builder().email("admin@sports.local")
                .passwordHash(encoder.encode("Admin@123")).fullName("Platform Super Admin").role(Role.SUPER_ADMIN).build()));
        User cricketAdmin=users.findByEmail("cricket.admin@sports.local").orElseGet(()->users.save(User.builder().email("cricket.admin@sports.local")
                .passwordHash(encoder.encode("Admin@123")).fullName("Cricket Championship Admin").role(Role.USER).build()));
        User footballAdmin=users.findByEmail("football.admin@sports.local").orElseGet(()->users.save(User.builder().email("football.admin@sports.local")
                .passwordHash(encoder.encode("Admin@123")).fullName("Football Championship Admin").role(Role.USER).build()));
        User captain=users.findByEmail("captain@sports.local").orElseGet(()->users.save(User.builder().email("captain@sports.local")
                .passwordHash(encoder.encode("Admin@123")).fullName("Mumbai Team Captain").role(Role.USER).build()));
        seedNavigation();
        if(championships.count()>0){ensureCaptainAccess(captain);return;}
        seedChampionship(admin,cricketAdmin,captain,"Premier Cricket League","CRICKET","CRIC-8821","cricket123",
                List.of("Mumbai Strikers","Delhi Capitals","Chennai Kings","Bengaluru Blasters"));
        seedChampionship(admin,footballAdmin,null,"Corporate Football Cup","FOOTBALL","FOOT-9922","football123",
                List.of("Goa United","Kerala FC","Kolkata Athletic","Pune City"));
    }

    private void seedNavigation() {
        List<ScreenSeed> catalog = List.of(
                new ScreenSeed("DASHBOARD", "Dashboard", "/dashboard", "LayoutDashboard", "Workspace", 10),
                new ScreenSeed("AUCTION", "Auction", "/auction", "Gavel", "Workspace", 20),
                new ScreenSeed("SCOREBOARD", "Scoreboard", "/scoreboard", "Trophy", "Workspace", 30),
                new ScreenSeed("MATCHES", "Matches", "/matches", "CalendarDays", "Workspace", 40),
                new ScreenSeed("TEAMS", "Teams", "/teams", "UsersRound", "Management", 50),
                new ScreenSeed("PLAYERS", "Players", "/players", "UserRound", "Management", 60),
                new ScreenSeed("ANNOUNCEMENTS", "Announcements", "/announcements", "Bell", "Management", 70),
                new ScreenSeed("ANALYSIS", "Team Analysis", "/analysis", "ChartNoAxesCombined", "Insights", 80),
                new ScreenSeed("FORMAT_LEADERS", "Format Leaders", "/format-leaders", "Medal", "Insights", 90),
                new ScreenSeed("TOP_PERFORMERS", "Top Performers", "/top-performers", "Sparkles", "Insights", 100),
                new ScreenSeed("HISTORY", "Auction History", "/history", "History", "Insights", 110),
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
                NavigationRole.SUPER_ADMIN, catalog.stream().map(ScreenSeed::code).collect(java.util.stream.Collectors.toSet()),
                NavigationRole.CHAMPIONSHIP_ADMIN, Set.of("DASHBOARD", "AUCTION", "SCOREBOARD", "MATCHES", "TEAMS", "PLAYERS", "ANNOUNCEMENTS", "ANALYSIS", "FORMAT_LEADERS", "TOP_PERFORMERS", "HISTORY"),
                NavigationRole.TEAM_CAPTAIN, Set.of("DASHBOARD", "AUCTION", "SCOREBOARD", "MATCHES", "TEAMS", "ANALYSIS", "FORMAT_LEADERS", "TOP_PERFORMERS", "HISTORY"),
                NavigationRole.SPECTATOR, Set.of("DASHBOARD", "AUCTION", "SCOREBOARD", "MATCHES", "TEAMS", "ANNOUNCEMENTS", "ANALYSIS", "FORMAT_LEADERS", "TOP_PERFORMERS")
        );
        grants.forEach((role, codes) -> codes.forEach(code -> {
            AppScreen screen = persisted.get(code);
            screenMappings.findByRoleAndScreenId(role, screen.getId()).orElseGet(() -> screenMappings.save(
                    RoleScreenMapping.builder().role(role).screen(screen).visible(true).build()));
        }));
    }

    private record ScreenSeed(String code, String label, String path, String icon, String section, int order) {}

    private void ensureCaptainAccess(User captain) {
        championships.findByRoomCodeIgnoreCase("CRIC-8821").ifPresent(championship ->
                teams.findAllByChampionshipIdOrderByName(championship.getId()).stream().findFirst().ifPresent(team -> {
                    ChampionshipRole access = roles.findByUserIdAndChampionshipId(captain.getId(), championship.getId()).orElseGet(ChampionshipRole::new);
                    access.setUser(captain); access.setChampionship(championship); access.setRole(ChampionshipRoleType.TEAM_CAPTAIN); access.setTeam(team);
                    roles.save(access);
                }));
    }

    private void seedChampionship(User owner,User manager,User captain,String name,String sport,String code,String passcode,List<String> teamNames){
        Championship c=championships.save(Championship.builder().name(name).sportType(sport).roomCode(code)
                .passcodeHash(encoder.encode(passcode)).isPublic(false).status(ChampionshipStatus.ACTIVE).createdBy(owner).build());
        settings.save(TournamentSettings.builder().championship(c).maxSquadSize(12).purseLimit(new BigDecimal("1000000000"))
                .customRules(sport.equals("CRICKET")?"{\"tieBreaker\":\"NET_RUN_RATE\"}":"{\"tieBreaker\":\"GOAL_DIFFERENCE\"}").build());
        Auction a=auctions.save(Auction.builder().championship(c).name(name+" Main Auction").build());
        states.save(AuctionState.builder().auction(a).status(AuctionStatus.NOT_STARTED).timerSeconds(30).build());
        roles.save(ChampionshipRole.builder().user(manager).championship(c).role(ChampionshipRoleType.CHAMPIONSHIP_ADMIN).build());
        int order=1;
        for(int i=0;i<teamNames.size();i++){
            String tn=teamNames.get(i); Team team=teams.save(Team.builder().championship(c).name(tn).shortCode(shortCode(tn))
                    .primaryColor(List.of("#7CFF6B","#62D9FF","#FFB347","#D58CFF").get(i)).purseTotal(new BigDecimal("1000000000"))
                    .purseRemaining(new BigDecimal("1000000000")).maleCount(0).femaleCount(0).build());
            if(i==0&&captain!=null)roles.save(ChampionshipRole.builder().user(captain).championship(c)
                    .role(ChampionshipRoleType.TEAM_CAPTAIN).team(team).build());
        }
        for(int i=1;i<=24;i++)players.save(Player.builder().championship(c).auction(a).fullName(sport.substring(0,1)+" Player "+i)
                .gender(i%4==0?Gender.FEMALE:Gender.MALE).basePrice(new BigDecimal("2000000")).status(PlayerStatus.AVAILABLE)
                .skillLevel(i%3==0?"ADVANCED":"INTERMEDIATE").auctionOrder(order++).build());
    }
    private String shortCode(String name){return Arrays.stream(name.split(" ")).map(s->s.substring(0,1)).reduce("",String::concat).toUpperCase();}
}
