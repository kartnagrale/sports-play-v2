package com.neml.badminton.controller;
import com.neml.badminton.dto.SportConfigurationDtos.*;import com.neml.badminton.entity.User;import com.neml.badminton.service.SportConfigurationService;import jakarta.validation.Valid;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.web.bind.annotation.*;import java.util.UUID;
import java.util.List;
@RestController @RequestMapping("/api/championships/{championshipId}/sport-configuration")
public class SportConfigurationController {
 private final SportConfigurationService service;public SportConfigurationController(SportConfigurationService service){this.service=service;}
 @GetMapping("/badminton")@PreAuthorize("@championshipSecurity.canView(#championshipId,authentication)")public BadmintonConfigDto badminton(@PathVariable UUID championshipId){return service.badminton(championshipId);}
 @PutMapping("/badminton")@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")public BadmintonConfigDto badminton(@PathVariable UUID championshipId,@Valid@RequestBody BadmintonConfigRequest r,@AuthenticationPrincipal User actor){return service.updateBadminton(championshipId,r,actor);}
 @GetMapping("/cricket")@PreAuthorize("@championshipSecurity.canView(#championshipId,authentication)")public CricketConfigDto cricket(@PathVariable UUID championshipId){return service.cricket(championshipId);}
 @PutMapping("/cricket")@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")public CricketConfigDto cricket(@PathVariable UUID championshipId,@Valid@RequestBody CricketConfigRequest r,@AuthenticationPrincipal User actor){return service.updateCricket(championshipId,r,actor);}
 @GetMapping("/quotas")@PreAuthorize("@championshipSecurity.canView(#championshipId,authentication)")public org.springframework.http.ResponseEntity<List<QuotaRuleDto>> quotas(@PathVariable UUID championshipId){var snapshot=service.quotaSnapshot(championshipId);return org.springframework.http.ResponseEntity.ok().eTag("\""+snapshot.version()+"\"").body(snapshot.rules());}
 @PutMapping("/quotas")@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")public List<QuotaRuleDto> quotas(@PathVariable UUID championshipId,@Valid@RequestBody List<@Valid QuotaRuleRequest> r,@RequestHeader("If-Match") int expectedVersion,@AuthenticationPrincipal User actor){return service.replaceQuotas(championshipId,r,expectedVersion,actor);}
}
