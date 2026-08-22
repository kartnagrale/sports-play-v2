package com.neml.badminton.controller;

import com.neml.badminton.dto.MatchDtos.*;
import com.neml.badminton.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/championships/{championshipId}")
public class TenantMatchController {
    private final MatchService service; public TenantMatchController(MatchService service){this.service=service;}
    @PostMapping("/admin/matches") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public MatchDto create(@PathVariable UUID championshipId,@Valid @RequestBody CreateMatchRequest req){return service.create(championshipId,req);}
    @PostMapping("/admin/matches/formats/{formatId}/assign") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public MatchDto assign(@PathVariable UUID championshipId,@PathVariable UUID formatId,@Valid @RequestBody AssignPlayersRequest req){return service.assignPlayers(championshipId,formatId,req);}
    @PostMapping("/admin/matches/formats/{formatId}/result") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public MatchDto result(@PathVariable UUID championshipId,@PathVariable UUID formatId,@Valid @RequestBody ReportFormatResultRequest req){return service.reportFormatResult(championshipId,formatId,req);}
    @PostMapping("/admin/matches/formats/{formatId}/live-score") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public MatchDto score(@PathVariable UUID championshipId,@PathVariable UUID formatId,@Valid @RequestBody ReportFormatResultRequest req){return service.reportLiveScore(championshipId,formatId,req);}
    @DeleteMapping("/admin/matches/{id}") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public ResponseEntity<Void> delete(@PathVariable UUID championshipId,@PathVariable UUID id){service.deleteMatch(championshipId,id);return ResponseEntity.noContent().build();}
}
