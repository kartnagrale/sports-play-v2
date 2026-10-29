package com.neml.badminton.service;

import com.neml.badminton.dto.ChampionshipDtos.*;
import com.neml.badminton.dto.ChampionshipDtos;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.security.JwtService;
import com.neml.badminton.competition.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ChampionshipService {
    private final ChampionshipRepository championships;
    private final ChampionshipRoleRepository roles;
    private final UserRepository users;
    private final AuctionRepository auctions;
    private final AuctionStateRepository states;
    private final TournamentSettingsRepository settings;
    private final TeamRepository teams;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MessageTrackerGateway messageTracker;
    private final AccountActivationService activations;
    private final CompetitionConfigurationRepository competitionConfigurations;
    private final SportEngineRegistry sportEngines;
    private final CricketConfigurationRepository cricketConfigurations;
    private final BadmintonConfigurationRepository badmintonConfigurations;
    private final ConfigurationSectionService configurationSections;
    private final ChampionshipBrandingConfigurationRepository brandingConfigurations;
    private final AuctionConfigurationRepository auctionConfigurations;
    private final AuctionPolicyGuard policyGuard;
    private final RosterQuotaRuleRepository quotaRules;

    public ChampionshipService(ChampionshipRepository championships, ChampionshipRoleRepository roles,
            UserRepository users, AuctionRepository auctions, AuctionStateRepository states,
            TournamentSettingsRepository settings, TeamRepository teams, PasswordEncoder encoder, JwtService jwt,
            MessageTrackerGateway messageTracker, AccountActivationService activations,
            CompetitionConfigurationRepository competitionConfigurations, SportEngineRegistry sportEngines,
            CricketConfigurationRepository cricketConfigurations,BadmintonConfigurationRepository badmintonConfigurations,ConfigurationSectionService configurationSections,ChampionshipBrandingConfigurationRepository brandingConfigurations,AuctionConfigurationRepository auctionConfigurations, AuctionPolicyGuard policyGuard, RosterQuotaRuleRepository quotaRules) {
        this.championships = championships; this.roles = roles; this.users = users; this.auctions = auctions;
        this.states = states; this.settings = settings; this.teams = teams; this.encoder = encoder; this.jwt = jwt;
        this.messageTracker = messageTracker;
        this.activations = activations;
        this.competitionConfigurations = competitionConfigurations;
        this.sportEngines = sportEngines;
        this.cricketConfigurations = cricketConfigurations;
        this.badmintonConfigurations = badmintonConfigurations;
        this.configurationSections=configurationSections;
        this.brandingConfigurations=brandingConfigurations;this.auctionConfigurations=auctionConfigurations;this.policyGuard=policyGuard;this.quotaRules=quotaRules;
    }

    @Transactional
    public CreateChampionshipResponse create(CreateChampionshipRequest req) {
        User creator = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String adminEmail = req.adminEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(adminEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this administrator email already exists");
        }
        User admin = users.save(User.builder()
                .email(adminEmail)
                .passwordHash(encoder.encode(UUID.randomUUID().toString())).accountEnabled(false)
                .fullName(req.adminFullName().trim())
                .role(Role.USER)
                .build());
        String code = uniqueCode(req.roomCode(), req.sportType());
        Championship c = championships.save(Championship.builder().name(req.name().trim())
                .sportType(req.sportType().trim().toUpperCase()).roomCode(code)
                .passcodeHash(blank(req.passcode()) ? null : encoder.encode(req.passcode()))
                .isPublic(Boolean.TRUE.equals(req.isPublic())).status(ChampionshipStatus.DRAFT)
                .lifecyclePhase(ChampionshipPhase.CONFIGURATION)
                .createdBy(creator).build());
        settings.save(TournamentSettings.builder().championship(c)
                .maxSquadSize(12).purseLimit(new BigDecimal("1000000000"))
                .minMale(9).minFemale(3).playerBasePrice(new BigDecimal("2000000"))
                .bidIncrement(new BigDecimal("500000")).timerSeconds(30).customRules("{}").build());
        roles.save(ChampionshipRole.builder().user(admin).championship(c)
                .role(ChampionshipRoleType.CHAMPIONSHIP_ADMIN).build());
        initializeConfigurations(c);
        var invitation = activations.issue(admin);
        messageTracker.queueChampionshipAdmin(admin, req.adminMobile().trim(), c, code);
        return new CreateChampionshipResponse(ChampionshipDtos.from(c, null),
                new ProvisionedAdminDto(admin.getId(), admin.getFullName(), admin.getEmail(),
                        req.adminMobile().trim(), invitation.setupPath(), invitation.expiresAt(), true));
    }

    @Transactional
    public List<ChampionshipDto> listFor(User user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff access required");
        List<Championship> list = user.getRole() == Role.SUPER_ADMIN ? championships.findAll()
                : roles.findAllByUserId(user.getId()).stream().map(ChampionshipRole::getChampionship).distinct().toList();
        return list.stream().map(c -> ChampionshipDtos.from(c, defaultAuction(c.getId()))).toList();
    }

    public ChampionshipDto get(UUID id) {
        Championship c = championships.findById(id).orElseThrow(() -> notFound("Championship"));
        return ChampionshipDtos.from(c, defaultAuction(id));
    }

    public JoinRoomResponse join(JoinRoomRequest req) {
        Championship c = championships.findByRoomCodeIgnoreCase(req.roomCode().trim())
                .orElseThrow(() -> notFound("Room"));
        if (!Boolean.TRUE.equals(c.getIsPublic())) {
            if (c.getPasscodeHash() == null || blank(req.passcode()) || !encoder.matches(req.passcode(), c.getPasscodeHash()))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid room passcode");
        }
        String token = jwt.generateToken("viewer:" + c.getId(), Map.of("token_type", "viewer",
                "championship_id", c.getId().toString(), "global_role", "VIEWER"));
        return new JoinRoomResponse(token, ChampionshipDtos.from(c, defaultAuction(c.getId())));
    }

    @Transactional
    public void assign(UUID championshipId, AssignRoleRequest req) {
        Championship c = championships.findById(championshipId).orElseThrow(() -> notFound("Championship"));
        User user = users.findByEmail(req.email().trim().toLowerCase()).orElseThrow(() -> notFound("User"));
        ChampionshipRole r = roles.findByUserIdAndChampionshipId(user.getId(), championshipId).orElseGet(ChampionshipRole::new);
        r.setUser(user); r.setChampionship(c); r.setRole(req.role());
        r.setTeam(req.teamId() == null ? null : teams.findByIdAndChampionshipId(req.teamId(), championshipId)
                .orElseThrow(() -> notFound("Team")));
        roles.save(r);
    }

    public ChampionshipSettingsDto settings(UUID championshipId) {
        Championship championship = championships.findById(championshipId).orElseThrow(() -> notFound("Championship"));
        TournamentSettings config = settings.findByChampionshipId(championshipId).orElseThrow(() -> notFound("Tournament settings"));
        Auction auction = auctions.findAllByChampionshipIdOrderByCreatedAtDesc(championshipId).stream().findFirst().orElse(null);
        return settingsDto(championship, config, auction);
    }

    @Transactional
    public ChampionshipSettingsDto configure(UUID championshipId, ConfigureChampionshipRequest req) {
        Championship championship = policyGuard.lockEditable(championshipId);
        AuctionConfiguration policy = auctionConfigurations.findLatestForUpdate(championshipId).orElseThrow(() -> notFound("Auction configuration"));
        if (!policy.getConfigurationVersion().equals(req.expectedVersion())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Configuration version conflict: reload before saving");
        if (req.minMale() + req.minFemale() > req.maxSquadSize()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Male and female minimums cannot exceed squad size");
        }
        TournamentSettings config = settings.findByChampionshipId(championshipId).orElseThrow(() -> notFound("Tournament settings"));
        List<Team> existingTeams = teams.findAllByChampionshipIdOrderByName(championshipId);
        Auction runningAuction = auctions.findAllByChampionshipIdOrderByCreatedAtDesc(championshipId).stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING || a.getStatus() == AuctionStatus.COMPLETED)
                .findFirst().orElse(null);
        if (runningAuction != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Live or completed championships cannot be reconfigured");
        }
        if (existingTeams.stream().anyMatch(t -> t.getPurseRemaining().compareTo(t.getPurseTotal()) != 0)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Purse settings cannot change after bidding starts");
        }
        config.setMaxSquadSize(req.maxSquadSize()); config.setPurseLimit(req.purseLimit());
        config.setMinMale(req.minMale()); config.setMinFemale(req.minFemale());
        config.setPlayerBasePrice(req.playerBasePrice()); config.setBidIncrement(req.bidIncrement());
        config.setTimerSeconds(req.timerSeconds()); config.setCustomRules(blank(req.customRules()) ? "{}" : req.customRules());
        settings.save(config);
        CompetitionConfiguration competition=competitionConfigurations.findByChampionshipIdForUpdate(championshipId).orElseThrow(() -> notFound("Competition configuration"));
        if(req.maxSquadSize()<competition.getPlayingSideSize()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"maxSquadSize must accommodate the configured playing side");
        competition.setMaximumRosterSize(req.maxSquadSize());
        competition.setMinimumRosterSize(Math.min(competition.getMinimumRosterSize(),req.maxSquadSize()));
        competition.setConfigurationVersion(competition.getConfigurationVersion()+1);
        competitionConfigurations.save(competition);
        policy.setConfigurationVersion(policy.getConfigurationVersion()+1);
        policy.setTeamBudget(req.purseLimit());policy.setMinimumBid(req.playerBasePrice());policy.setBidIncrement(req.bidIncrement());
        policy.setMinimumRosterSize(competition.getMinimumRosterSize());policy.setMaximumRosterSize(req.maxSquadSize());policy.setTimerSeconds(req.timerSeconds());
        auctionConfigurations.save(policy);
        // Keep non-gender quota rules; replace the squad gender rules in a new version.
        List<RosterQuotaRule> current=quotaRules.findAllByChampionshipIdOrderByRuleCode(championshipId);
        int quotaVersion=quotaRules.findLatestConfigurationVersion(championshipId)+1;
        List<RosterQuotaRule> next=new ArrayList<>();
        for(RosterQuotaRule r:current) if(!("GENDER".equals(r.getCategoryType())&&"SQUAD".equals(r.getAppliesTo())))
            next.add(RosterQuotaRule.builder().championship(championship).configurationVersion(quotaVersion).sportCode(championship.getSportType()).ruleCode(r.getRuleCode()).categoryType(r.getCategoryType()).categoryValue(r.getCategoryValue()).minimumCount(r.getMinimumCount()).maximumCount(r.getMaximumCount()).appliesTo(r.getAppliesTo()).enabled(r.getEnabled()).validationMessage(r.getValidationMessage()).build());
        for(Gender gender:Gender.values()) next.add(RosterQuotaRule.builder().championship(championship).configurationVersion(quotaVersion).sportCode(championship.getSportType()).ruleCode("SQUAD_"+gender).categoryType("GENDER").categoryValue(gender.name()).minimumCount(gender==Gender.FEMALE?req.minFemale():req.minMale()).appliesTo("SQUAD").enabled(true).build());
        quotaRules.saveAll(next);
        existingTeams.forEach(team -> { team.setPurseTotal(req.purseLimit()); team.setPurseRemaining(req.purseLimit()); });
        teams.saveAll(existingTeams);
        Auction auction = auctions.findAllByChampionshipIdOrderByCreatedAtDesc(championshipId).stream().findFirst()
                .orElseGet(() -> auctions.save(Auction.builder().championship(championship).name(req.auctionName().trim()).build()));
        auction.setName(req.auctionName().trim()); auctions.save(auction);
        AuctionState state = states.findByAuctionIdAndChampionshipId(auction.getId(), championshipId).orElseGet(() -> AuctionState.builder()
                .championship(championship).auction(auction).status(AuctionStatus.NOT_STARTED).build());
        state.setChampionship(championship);
        state.setTimerSeconds(req.timerSeconds()); states.save(state);
        championship.setStatus(ChampionshipStatus.ACTIVE); championships.save(championship);
        championship.setLifecyclePhase(ChampionshipPhase.TEAM_PROVISIONING);
        championships.save(championship);
        return settingsDto(championship, config, auction);
    }

    @Transactional
    public ProvisionedTeamDto createTeamCaptain(UUID championshipId, CreateTeamCaptainRequest req) {
        Championship championship = championships.findByIdForUpdate(championshipId).orElseThrow(() -> notFound("Championship"));
        if (championship.getStatus() != ChampionshipStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Configure the championship before creating teams");
        }
        CompetitionConfiguration competition=competitionConfigurations.findByChampionshipIdForUpdate(championshipId)
                .orElseThrow(()->notFound("Competition configuration"));
        int existingTeamCount=teams.findAllByChampionshipIdOrderByName(championshipId).size();
        if(existingTeamCount>=competition.getMaximumTeamCount()||existingTeamCount>=competition.getConfiguredTeamCount())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Configured team limit of "+competition.getConfiguredTeamCount()+" has been reached");
        String email = req.captainEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this captain email already exists");
        if (teams.findAllByChampionshipIdOrderByName(championshipId).stream().anyMatch(t -> t.getName().equalsIgnoreCase(req.teamName().trim()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A team with this name already exists");
        }
        if (teams.findAllByChampionshipIdOrderByName(championshipId).stream().anyMatch(t -> t.getShortCode().equalsIgnoreCase(req.shortCode().trim()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A team with this short code already exists");
        }
        TournamentSettings config = settings.findByChampionshipId(championshipId).orElseThrow(() -> notFound("Tournament settings"));
        User captain = users.save(User.builder().email(email).passwordHash(encoder.encode(UUID.randomUUID().toString())).accountEnabled(false)
                .fullName(req.captainFullName().trim()).role(Role.USER).build());
        Team team = teams.save(Team.builder().championship(championship).name(req.teamName().trim())
                .shortCode(req.shortCode().trim().toUpperCase(Locale.ROOT)).logoUrl(req.logoUrl())
                .primaryColor(blank(req.primaryColor()) ? "#7CFF6B" : req.primaryColor())
                .purseTotal(config.getPurseLimit()).purseRemaining(config.getPurseLimit())
                .maleCount(0).femaleCount(0).matchPoints(0).build());
        configurationSections.lock(championshipId,"BASICS",null);
        roles.save(ChampionshipRole.builder().user(captain).championship(championship)
                .role(ChampionshipRoleType.TEAM_CAPTAIN).team(team).build());
        if (championship.getLifecyclePhase().ordinal() < ChampionshipPhase.TEAM_PROVISIONING.ordinal()) {
            championship.setLifecyclePhase(ChampionshipPhase.TEAM_PROVISIONING);
            championships.save(championship);
        }
        var invitation = activations.issue(captain);
        messageTracker.queueTeamCaptain(captain, req.captainMobile().trim(), championship, team);
        return new ProvisionedTeamDto(team.getId(), team.getName(), team.getShortCode(), captain.getId(),
                captain.getFullName(), captain.getEmail(), req.captainMobile().trim(), invitation.setupPath(), invitation.expiresAt(), true);
    }

    private ChampionshipSettingsDto settingsDto(Championship championship, TournamentSettings config, Auction auction) {
        return new ChampionshipSettingsDto(championship.getId(), championship.getStatus(),
                auction == null ? "Main Auction" : auction.getName(), config.getMaxSquadSize(), config.getPurseLimit(),
                config.getMinMale(), config.getMinFemale(), config.getPlayerBasePrice(), config.getBidIncrement(),
                config.getTimerSeconds(), config.getCustomRules(), auction == null ? null : auction.getId(), auctionConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(championship.getId()).orElseThrow(() -> notFound("Auction configuration")).getConfigurationVersion());
    }

    private UUID defaultAuction(UUID championshipId) {
        return auctions.findAllByChampionshipIdOrderByCreatedAtDesc(championshipId).stream().findFirst().map(Auction::getId).orElse(null);
    }

    @Transactional
    public AccountActivationService.Invitation reissueSetup(UUID championshipId,UUID userId,User actor){
        ChampionshipRole membership=roles.findByUserIdAndChampionshipId(userId,championshipId)
                .orElseThrow(()->notFound("Account"));
        boolean allowed=actor!=null&&(actor.getRole()==Role.SUPER_ADMIN
                ?membership.getRole()==ChampionshipRoleType.CHAMPIONSHIP_ADMIN
                :membership.getRole()==ChampionshipRoleType.TEAM_CAPTAIN);
        if(!allowed)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Account setup is not permitted");
        return activations.issue(membership.getUser());
    }

    /** Initializes missing records for new championships and local demo seeds without replacing saved settings. */
    @Transactional
    public void initializeConfigurations(Championship championship) {
        UUID id = championship.getId();
        String sport = SportEngineRegistry.normalize(championship.getSportType());
        if (competitionConfigurations.findByChampionshipId(id).isEmpty()) {
            createCompetitionConfiguration(championship);
        }
        configurationSections.initialize(championship);
        if (brandingConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(id).isEmpty()) {
            brandingConfigurations.save(ChampionshipBrandingConfiguration.builder().championship(championship)
                    .configurationVersion(1).displayName(championship.getName())
                    .shortName(championship.getName().substring(0, Math.min(32, championship.getName().length())))
                    .publicVisibility(championship.getIsPublic()).build());
        }
        if (auctionConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(id).isEmpty()) {
            auctionConfigurations.save(AuctionConfiguration.builder().championship(championship)
                    .configurationVersion(1).teamBudget(new BigDecimal("1000000000"))
                    .minimumBid(new BigDecimal("2000000")).bidIncrement(new BigDecimal("500000"))
                    .minimumRosterSize("CRICKET".equals(sport) ? 14 : 8)
                    .maximumRosterSize("CRICKET".equals(sport) ? 18 : 12).build());
        }
        if ("CRICKET".equals(sport)
                && cricketConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(id).isEmpty()) {
            cricketConfigurations.save(CricketConfiguration.builder().championship(championship).configurationVersion(1).build());
        } else if ("BADMINTON".equals(sport)
                && badmintonConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(id).isEmpty()) {
            createBadmintonConfiguration(championship);
        }
    }

    private void createCompetitionConfiguration(Championship championship) {
        SportCompetitionEngine.CompetitionDefaults defaults = sportEngines.supports(championship.getSportType())
                ? sportEngines.require(championship.getSportType()).defaultConfiguration()
                : new SportCompetitionEngine.CompetitionDefaults(RoundRobinMode.SINGLE, 3, 0,
                "MATCHES_WON,NAME", 2, 1, true, List.of("Main Venue"));
        boolean cricket="CRICKET".equals(SportEngineRegistry.normalize(championship.getSportType()));
        competitionConfigurations.save(CompetitionConfiguration.builder()
                .championship(championship).sportCode(SportEngineRegistry.normalize(championship.getSportType()))
                .minimumRosterSize(cricket?14:8).maximumRosterSize(cricket?18:12)
                .playingSideSize(cricket?11:7).presetCode("CUSTOM_CHAMPIONSHIP")
                .roundRobinMode(defaults.roundRobinMode()).pointsPerWin(defaults.pointsPerWin())
                .pointsPerLoss(defaults.pointsPerLoss()).tieBreakerPriority(defaults.tieBreakerPriority())
                .qualifyingTeamCount(defaults.qualifyingTeamCount())
                .venues(String.join("\n", defaults.venues())).fixtureSpacingDays(defaults.fixtureSpacingDays())
                .manualDateEditing(defaults.manualDateEditing()).resultSchemaVersion(1).build());
    }
    private void createBadmintonConfiguration(Championship championship){
        BadmintonConfiguration config=BadmintonConfiguration.builder().championship(championship).configurationVersion(1).build();
        List<Object[]> defaults=List.of(
                new Object[]{"MENS_SINGLES","Men's Singles","MS",1,1,"MALE"},
                new Object[]{"WOMENS_SINGLES","Women's Singles","WS",2,1,"FEMALE"},
                new Object[]{"MENS_DOUBLES","Men's Doubles","MD",3,2,"MALE"},
                new Object[]{"MIXED_DOUBLES","Mixed Doubles","XD",4,2,"MIXED"},
                new Object[]{"MENS_DOUBLES_TWO","Men's Doubles II","MD2",5,2,"MALE"});
        defaults.forEach(v->config.getRubbers().add(BadmintonRubberRule.builder().configuration(config).rubberCode((String)v[0]).displayLabel((String)v[1]).shortLabel((String)v[2]).displayOrder((Integer)v[3]).participantCountPerSide((Integer)v[4]).genderRule((String)v[5]).build()));
        badmintonConfigurations.save(config);
    }
    private String uniqueCode(String requested, String sport) {
        String prefix = blank(requested) ? sport.replaceAll("[^A-Za-z]", "").toUpperCase() : requested.toUpperCase();
        prefix = prefix.substring(0, Math.min(4, prefix.length()));
        String code = blank(requested) ? null : requested.trim().toUpperCase();
        do { if (code == null || championships.existsByRoomCodeIgnoreCase(code)) code = prefix + "-" + (1000 + new Random().nextInt(9000)); }
        while (championships.existsByRoomCodeIgnoreCase(code));
        return code;
    }
    private boolean blank(String s) { return s == null || s.isBlank(); }
    private ResponseStatusException notFound(String what) { return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found"); }

}
