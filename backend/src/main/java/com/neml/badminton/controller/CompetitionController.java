package com.neml.badminton.controller;

import com.neml.badminton.dto.CompetitionDtos.*;
import com.neml.badminton.entity.User;
import com.neml.badminton.entity.ChampionshipPhase;
import com.neml.badminton.service.CompetitionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/championships/{championshipId}")
public class CompetitionController {
    private final CompetitionService service;
    public CompetitionController(CompetitionService service) { this.service=service; }

    @GetMapping("/competition")
    @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public CompetitionOverviewDto overview(@PathVariable UUID championshipId) {
        return service.overview(championshipId);
    }
    @GetMapping("/competition/configuration")
    @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public CompetitionConfigurationDto configuration(@PathVariable UUID championshipId) {
        return service.configuration(championshipId);
    }
    @GetMapping("/competition/capabilities")
    @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public SportCapabilitiesDto capabilities(@PathVariable UUID championshipId) {
        return service.capabilities(championshipId);
    }
    @GetMapping("/competition/audit")
    @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public List<AuditEventDto> audit(@PathVariable UUID championshipId) {
        return service.auditHistory(championshipId);
    }
    @GetMapping("/dashboard")
    @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public DashboardCapabilitiesDto dashboard(@PathVariable UUID championshipId, Authentication authentication) {
        return service.dashboard(championshipId,authentication);
    }
    @PutMapping("/admin/competition/configuration")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public CompetitionConfigurationDto configure(@PathVariable UUID championshipId,
            @Valid @RequestBody CompetitionConfigurationRequest request,@AuthenticationPrincipal User actor) {
        return service.configure(championshipId,request,actor);
    }
    @PostMapping("/admin/competition/league/generate")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public CompetitionOverviewDto league(@PathVariable UUID championshipId,@AuthenticationPrincipal User actor) {
        return service.generateLeague(championshipId,actor);
    }
    @PostMapping("/admin/competition/playoffs/generate")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public CompetitionOverviewDto playoffs(@PathVariable UUID championshipId,@AuthenticationPrincipal User actor) {
        return service.generatePlayoffs(championshipId,actor);
    }
    @PatchMapping("/admin/competition/fixtures/{fixtureId}/schedule")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public com.neml.badminton.dto.MatchDtos.MatchDto reschedule(@PathVariable UUID championshipId,
            @PathVariable UUID fixtureId,@Valid @RequestBody RescheduleFixtureRequest request,
            @AuthenticationPrincipal User actor) {
        return service.reschedule(championshipId,fixtureId,request,actor);
    }
    @GetMapping("/configuration/workspace")
    @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public ConfigurationWorkspaceDto workspace(@PathVariable UUID championshipId,Authentication authentication){return service.workspace(championshipId,authentication);}
    @PostMapping("/admin/configuration/validate")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public ConfigurationWorkspaceDto validate(@PathVariable UUID championshipId,@AuthenticationPrincipal User actor,Authentication authentication){return service.validateConfiguration(championshipId,actor,authentication);}
    @PostMapping("/admin/configuration/reopen")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public ConfigurationWorkspaceDto reopen(@PathVariable UUID championshipId,@Valid@RequestBody ConfigurationActionRequest request,@AuthenticationPrincipal User actor,Authentication authentication){return service.reopenConfiguration(championshipId,request,actor,authentication);}
    @GetMapping("/phase-readiness")
    @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public PhaseReadinessDto readiness(@PathVariable UUID championshipId,@RequestParam ChampionshipPhase target){return service.phaseReadiness(championshipId,target);}
    @PostMapping("/admin/phases/{target}/advance")
    @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public PhaseReadinessDto advance(@PathVariable UUID championshipId,@PathVariable ChampionshipPhase target,@AuthenticationPrincipal User actor){return service.advancePhase(championshipId,target,actor);}
}
