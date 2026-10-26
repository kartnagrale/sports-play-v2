package com.neml.badminton.controller;

import com.neml.badminton.dto.ChampionshipDtos.*;
import com.neml.badminton.entity.User;
import com.neml.badminton.service.ChampionshipService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/championships")
public class ChampionshipController {
    private final ChampionshipService service;
    public ChampionshipController(ChampionshipService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public CreateChampionshipResponse create(@Valid @RequestBody CreateChampionshipRequest req) {
        return service.create(req);
    }
    @GetMapping public List<ChampionshipDto> list(@AuthenticationPrincipal User user) { return service.listFor(user); }
    @PostMapping("/join-room") public JoinRoomResponse join(@Valid @RequestBody JoinRoomRequest req) { return service.join(req); }

    @GetMapping("/{championshipId}")
    @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public ChampionshipDto get(@PathVariable UUID championshipId) { return service.get(championshipId); }

    @PostMapping("/{championshipId}/roles")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public ResponseEntity<Void> assign(@PathVariable UUID championshipId, @Valid @RequestBody AssignRoleRequest req) {
        service.assign(championshipId, req); return ResponseEntity.noContent().build();
    }

    @GetMapping("/{championshipId}/settings")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public ChampionshipSettingsDto settings(@PathVariable UUID championshipId) { return service.settings(championshipId); }

    @PutMapping("/{championshipId}/settings")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public ChampionshipSettingsDto configure(@PathVariable UUID championshipId,
                                              @Valid @RequestBody ConfigureChampionshipRequest req) {
        return service.configure(championshipId, req);
    }

    @PostMapping("/{championshipId}/admin/teams")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public ProvisionedTeamDto createTeamCaptain(@PathVariable UUID championshipId,
                                                @Valid @RequestBody CreateTeamCaptainRequest req) {
        return service.createTeamCaptain(championshipId, req);
    }
}
