package com.neml.badminton.controller;

import com.neml.badminton.dto.SquadConfirmationDtos.*;
import com.neml.badminton.entity.User;
import com.neml.badminton.service.SquadConfirmationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/championships/{championshipId}/squad-confirmations")
public class SquadConfirmationController {
    private final SquadConfirmationService service;
    public SquadConfirmationController(SquadConfirmationService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public SquadSummaryDto summary(@PathVariable UUID championshipId) { return service.summary(championshipId); }

    @GetMapping("/history")
    @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public List<SquadEventDto> history(@PathVariable UUID championshipId) { return service.history(championshipId); }

    @PostMapping("/teams/{teamId}/confirm")
    @PreAuthorize("@championshipSecurity.canConfirmSquad(#championshipId, #teamId, authentication)")
    public SquadSummaryDto confirm(@PathVariable UUID championshipId, @PathVariable UUID teamId,
            @AuthenticationPrincipal User actor, @Valid @RequestBody(required = false) SquadActionRequest request) {
        return service.confirm(championshipId, teamId, actor, request);
    }

    @PostMapping("/teams/{teamId}/lock")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public SquadSummaryDto lock(@PathVariable UUID championshipId, @PathVariable UUID teamId,
            @AuthenticationPrincipal User actor, @Valid @RequestBody(required = false) SquadActionRequest request) {
        return service.lock(championshipId, teamId, actor, request);
    }

    @PostMapping("/lock-all")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public SquadSummaryDto lockAll(@PathVariable UUID championshipId, @AuthenticationPrincipal User actor,
            @Valid @RequestBody(required = false) SquadActionRequest request) {
        return service.lockAll(championshipId, actor, request);
    }

    @PostMapping("/teams/{teamId}/reopen")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public SquadSummaryDto reopen(@PathVariable UUID championshipId, @PathVariable UUID teamId,
            @AuthenticationPrincipal User actor, @Valid @RequestBody(required = false) SquadActionRequest request) {
        return service.reopen(championshipId, teamId, actor, request);
    }
}
