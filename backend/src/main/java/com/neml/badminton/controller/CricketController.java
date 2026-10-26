package com.neml.badminton.controller;
import com.neml.badminton.dto.CricketDtos.*; import com.neml.badminton.entity.User; import com.neml.badminton.service.CricketScoringService;
import jakarta.validation.Valid; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*; import java.util.UUID;
@RestController @RequestMapping("/api/championships/{championshipId}/cricket/matches/{matchId}")
public class CricketController {
 private final CricketScoringService service; public CricketController(CricketScoringService service){this.service=service;}
 @PutMapping("/playing-xi/{teamId}") @PreAuthorize("@championshipSecurity.canBid(#championshipId,#teamId,authentication)")
 public void xi(@PathVariable UUID championshipId,@PathVariable UUID matchId,@PathVariable UUID teamId,@Valid @RequestBody PlayingXiRequest r,@AuthenticationPrincipal User actor){service.lockXi(championshipId,matchId,teamId,r,actor);}
 @PostMapping("/toss") @PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
 public MatchStateDto toss(@PathVariable UUID championshipId,@PathVariable UUID matchId,@Valid @RequestBody TossRequest r,@AuthenticationPrincipal User actor){return service.toss(championshipId,matchId,r,actor);}
 @PostMapping("/innings") @PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
 public MatchStateDto innings(@PathVariable UUID championshipId,@PathVariable UUID matchId,@Valid @RequestBody StartInningsRequest r,@AuthenticationPrincipal User actor){return service.startInnings(championshipId,matchId,r,actor);}
 @PostMapping("/innings/{inningsId}/deliveries") @PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
 public MatchStateDto delivery(@PathVariable UUID championshipId,@PathVariable UUID matchId,@PathVariable UUID inningsId,@Valid @RequestBody DeliveryRequest r,@AuthenticationPrincipal User actor){return service.delivery(championshipId,matchId,inningsId,r,actor);}
 @DeleteMapping("/innings/{inningsId}/deliveries/{deliveryId}") @PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
 public MatchStateDto correct(@PathVariable UUID championshipId,@PathVariable UUID matchId,@PathVariable UUID inningsId,@PathVariable UUID deliveryId,@Valid @RequestBody DeliveryCorrectionRequest r,@AuthenticationPrincipal User actor){return service.correctDelivery(championshipId,matchId,inningsId,deliveryId,r,actor);}
 @PutMapping("/target") @PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
 public MatchStateDto target(@PathVariable UUID championshipId,@PathVariable UUID matchId,@Valid@RequestBody TargetOverrideRequest r,@AuthenticationPrincipal User actor){return service.overrideTarget(championshipId,matchId,r,actor);}
 @GetMapping @PreAuthorize("@championshipSecurity.canView(#championshipId,authentication)")
 public MatchStateDto state(@PathVariable UUID championshipId,@PathVariable UUID matchId){return service.stateView(championshipId,matchId);}
 @PostMapping("/administrative-result") @PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
 public MatchStateDto administrative(@PathVariable UUID championshipId,@PathVariable UUID matchId,@Valid@RequestBody AdministrativeResultRequest r,@AuthenticationPrincipal User actor){return service.administrativeResult(championshipId,matchId,r,actor);}
}
