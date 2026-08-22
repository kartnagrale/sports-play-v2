package com.neml.badminton.controller;

import com.neml.badminton.dto.NavigationDtos.NavigationResponse;
import com.neml.badminton.service.NavigationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/navigation")
public class NavigationController {
    private final NavigationService service;

    public NavigationController(NavigationService service) { this.service = service; }

    @GetMapping
    public NavigationResponse navigation(@RequestParam(required = false) UUID championshipId,
                                         Authentication authentication) {
        return service.navigation(championshipId, authentication);
    }
}
