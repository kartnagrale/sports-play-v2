package com.neml.badminton.controller;

import com.neml.badminton.dto.MatchDtos.*;
import com.neml.badminton.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/championships/{championshipId}/analytics")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
public class TenantAnalyticsController {
    private final AnalyticsService service; public TenantAnalyticsController(AnalyticsService service){this.service=service;}
    @GetMapping("/standings") public List<StandingDto> standings(@PathVariable UUID championshipId,@RequestParam(defaultValue="true") boolean penalties){return service.standings(championshipId,penalties);}
    @GetMapping("/format-leaders") public List<FormatLeaderDto> formats(@PathVariable UUID championshipId){return service.formatLeaders(championshipId);}
    @GetMapping("/team/{id}") public TeamAnalysisDto team(@PathVariable UUID championshipId,@PathVariable UUID id){return service.teamAnalysis(championshipId,id);}
    @GetMapping("/top-performers") public List<TopPerformerDto> performers(@PathVariable UUID championshipId,@RequestParam(defaultValue="10") int limit){return service.topPerformers(championshipId,limit);}
}
