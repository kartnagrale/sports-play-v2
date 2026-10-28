package com.neml.badminton.controller;
import com.neml.badminton.service.DashboardAnalyticsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DashboardAnalyticsController {
    private final DashboardAnalyticsService service;
    public DashboardAnalyticsController(DashboardAnalyticsService service){this.service=service;}
    @GetMapping("/platform/analytics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public DashboardAnalyticsService.Platform platform(){return service.platform();}
    @GetMapping("/championships/{championshipId}/analytics/summary")
    @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public DashboardAnalyticsService.Summary summary(@PathVariable UUID championshipId,Authentication authentication){return service.summary(championshipId,authentication);}
}
