package com.neml.badminton.service;

import com.neml.badminton.dto.ChampionshipDtos.*;
import com.neml.badminton.dto.ChampionshipDtos;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.security.JwtService;
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
    private final TemporaryPasswordGenerator passwords;

    public ChampionshipService(ChampionshipRepository championships, ChampionshipRoleRepository roles,
            UserRepository users, AuctionRepository auctions, AuctionStateRepository states,
            TournamentSettingsRepository settings, TeamRepository teams, PasswordEncoder encoder, JwtService jwt,
            MessageTrackerGateway messageTracker, TemporaryPasswordGenerator passwords) {
        this.championships = championships; this.roles = roles; this.users = users; this.auctions = auctions;
        this.states = states; this.settings = settings; this.teams = teams; this.encoder = encoder; this.jwt = jwt;
        this.messageTracker = messageTracker;
        this.passwords = passwords;
    }

    @Transactional
    public CreateChampionshipResponse create(CreateChampionshipRequest req) {
        User creator = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String adminEmail = req.adminEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(adminEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this administrator email already exists");
        }
        String temporaryPassword = passwords.generate();
        User admin = users.save(User.builder()
                .email(adminEmail)
                .passwordHash(encoder.encode(temporaryPassword))
                .fullName(req.adminFullName().trim())
                .role(Role.USER)
                .build());
        String code = uniqueCode(req.roomCode(), req.sportType());
        Championship c = championships.save(Championship.builder().name(req.name().trim())
                .sportType(req.sportType().trim().toUpperCase()).roomCode(code)
                .passcodeHash(blank(req.passcode()) ? null : encoder.encode(req.passcode()))
                .isPublic(Boolean.TRUE.equals(req.isPublic())).status(ChampionshipStatus.DRAFT)
                .createdBy(creator).build());
        settings.save(TournamentSettings.builder().championship(c)
                .maxSquadSize(12).purseLimit(new BigDecimal("1000000000"))
                .minMale(9).minFemale(3).playerBasePrice(new BigDecimal("2000000"))
                .bidIncrement(new BigDecimal("500000")).timerSeconds(30).customRules("{}").build());
        roles.save(ChampionshipRole.builder().user(admin).championship(c)
                .role(ChampionshipRoleType.CHAMPIONSHIP_ADMIN).build());
        messageTracker.queueChampionshipAdmin(admin, req.adminMobile().trim(), temporaryPassword, c, code);
        return new CreateChampionshipResponse(ChampionshipDtos.from(c, null),
                new ProvisionedAdminDto(admin.getId(), admin.getFullName(), admin.getEmail(),
                        req.adminMobile().trim(), temporaryPassword, true));
    }

    public List<ChampionshipDto> listFor(User user) {
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
        Championship championship = championships.findByIdForUpdate(championshipId).orElseThrow(() -> notFound("Championship"));
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
        return settingsDto(championship, config, auction);
    }

    @Transactional
    public ProvisionedTeamDto createTeamCaptain(UUID championshipId, CreateTeamCaptainRequest req) {
        Championship championship = championships.findById(championshipId).orElseThrow(() -> notFound("Championship"));
        if (championship.getStatus() != ChampionshipStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Configure the championship before creating teams");
        }
        String email = req.captainEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this captain email already exists");
        if (teams.findAllByChampionshipIdOrderByName(championshipId).stream().anyMatch(t -> t.getName().equalsIgnoreCase(req.teamName().trim()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A team with this name already exists");
        }
        if (teams.findAllByChampionshipIdOrderByName(championshipId).stream().anyMatch(t -> t.getShortCode().equalsIgnoreCase(req.shortCode().trim()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A team with this short code already exists");
        }
        TournamentSettings config = settings.findByChampionshipId(championshipId).orElseThrow(() -> notFound("Tournament settings"));
        String temporaryPassword = passwords.generate();
        User captain = users.save(User.builder().email(email).passwordHash(encoder.encode(temporaryPassword))
                .fullName(req.captainFullName().trim()).role(Role.USER).build());
        Team team = teams.save(Team.builder().championship(championship).name(req.teamName().trim())
                .shortCode(req.shortCode().trim().toUpperCase(Locale.ROOT)).logoUrl(req.logoUrl())
                .primaryColor(blank(req.primaryColor()) ? "#7CFF6B" : req.primaryColor())
                .purseTotal(config.getPurseLimit()).purseRemaining(config.getPurseLimit())
                .maleCount(0).femaleCount(0).matchPoints(0).build());
        roles.save(ChampionshipRole.builder().user(captain).championship(championship)
                .role(ChampionshipRoleType.TEAM_CAPTAIN).team(team).build());
        messageTracker.queueTeamCaptain(captain, req.captainMobile().trim(), temporaryPassword, championship, team);
        return new ProvisionedTeamDto(team.getId(), team.getName(), team.getShortCode(), captain.getId(),
                captain.getFullName(), captain.getEmail(), req.captainMobile().trim(), temporaryPassword, true);
    }

    private ChampionshipSettingsDto settingsDto(Championship championship, TournamentSettings config, Auction auction) {
        return new ChampionshipSettingsDto(championship.getId(), championship.getStatus(),
                auction == null ? "Main Auction" : auction.getName(), config.getMaxSquadSize(), config.getPurseLimit(),
                config.getMinMale(), config.getMinFemale(), config.getPlayerBasePrice(), config.getBidIncrement(),
                config.getTimerSeconds(), config.getCustomRules(), auction == null ? null : auction.getId());
    }

    private UUID defaultAuction(UUID championshipId) {
        return auctions.findAllByChampionshipIdOrderByCreatedAtDesc(championshipId).stream().findFirst().map(Auction::getId).orElse(null);
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
