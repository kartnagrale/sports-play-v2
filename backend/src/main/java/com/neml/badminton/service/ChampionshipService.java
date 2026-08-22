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

    public ChampionshipService(ChampionshipRepository championships, ChampionshipRoleRepository roles,
            UserRepository users, AuctionRepository auctions, AuctionStateRepository states,
            TournamentSettingsRepository settings, TeamRepository teams, PasswordEncoder encoder, JwtService jwt) {
        this.championships = championships; this.roles = roles; this.users = users; this.auctions = auctions;
        this.states = states; this.settings = settings; this.teams = teams; this.encoder = encoder; this.jwt = jwt;
    }

    @Transactional
    public ChampionshipDto create(CreateChampionshipRequest req) {
        User creator = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String code = uniqueCode(req.roomCode(), req.sportType());
        Championship c = championships.save(Championship.builder().name(req.name().trim())
                .sportType(req.sportType().trim().toUpperCase()).roomCode(code)
                .passcodeHash(blank(req.passcode()) ? null : encoder.encode(req.passcode()))
                .isPublic(Boolean.TRUE.equals(req.isPublic())).status(ChampionshipStatus.ACTIVE)
                .createdBy(creator).build());
        settings.save(TournamentSettings.builder().championship(c)
                .maxSquadSize(req.maxSquadSize() == null ? 12 : req.maxSquadSize())
                .purseLimit(req.purseLimit() == null ? new BigDecimal("1000000000") : req.purseLimit())
                .customRules(blank(req.customRules()) ? "{}" : req.customRules()).build());
        Auction auction = auctions.save(Auction.builder().championship(c).name("Main Auction").build());
        states.save(AuctionState.builder().auction(auction).status(AuctionStatus.NOT_STARTED).timerSeconds(30).build());
        roles.save(ChampionshipRole.builder().user(creator).championship(c)
                .role(ChampionshipRoleType.CHAMPIONSHIP_ADMIN).build());
        return ChampionshipDtos.from(c, auction.getId());
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
