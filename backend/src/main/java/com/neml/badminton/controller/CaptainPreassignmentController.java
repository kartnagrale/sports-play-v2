package com.neml.badminton.controller;
import com.neml.badminton.dto.Dtos.PlayerDto;
import com.neml.badminton.entity.User;
import com.neml.badminton.service.CaptainPreassignmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/api/championships/{championshipId}/admin")
public class CaptainPreassignmentController {
    private final CaptainPreassignmentService service;
    public CaptainPreassignmentController(CaptainPreassignmentService service){this.service=service;}
    @GetMapping("/captain-players") @PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
    public java.util.List<CaptainPreassignmentService.Assignment> assignments(@PathVariable UUID championshipId){return service.assignments(championshipId);}
    @PutMapping("/teams/{teamId}/captains/{captainId}/player") @PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
    public PlayerDto assign(@PathVariable UUID championshipId,@PathVariable UUID teamId,@PathVariable UUID captainId,
            @Valid @RequestBody CaptainPreassignmentService.Request request,@AuthenticationPrincipal User actor) {
        return service.assign(championshipId,teamId,captainId,request,actor);
    }
}
