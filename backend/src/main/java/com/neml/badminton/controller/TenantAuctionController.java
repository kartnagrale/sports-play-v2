package com.neml.badminton.controller;

import com.neml.badminton.dto.Dtos.*;
import com.neml.badminton.service.TenantAuctionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/championships/{championshipId}/auctions/{auctionId}")
public class TenantAuctionController {
    private final TenantAuctionService service;
    public TenantAuctionController(TenantAuctionService service){this.service=service;}
    @GetMapping("/state") @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public AuctionStateDto state(@PathVariable UUID championshipId,@PathVariable UUID auctionId){return service.state(championshipId,auctionId);}
    @GetMapping("/history") @PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
    public List<BidDto> history(@PathVariable UUID championshipId,@PathVariable UUID auctionId){return service.history(championshipId,auctionId);}
    @PostMapping("/bid") @PreAuthorize("@championshipSecurity.canBid(#championshipId, #req.teamId(), authentication)")
    public AuctionStateDto bid(@PathVariable UUID championshipId,@PathVariable UUID auctionId,@Valid @RequestBody PlaceBidRequest req){return service.bid(championshipId,auctionId,req);}
    @PostMapping("/admin/{action}") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public AuctionStateDto action(@PathVariable UUID championshipId,@PathVariable UUID auctionId,@PathVariable String action){
        return switch(action){case "start"->service.start(championshipId,auctionId);case "pause"->service.pause(championshipId,auctionId);case "resume"->service.resume(championshipId,auctionId);case "undo"->service.undo(championshipId,auctionId);case "sell"->service.sell(championshipId,auctionId);case "unsold"->service.unsold(championshipId,auctionId);case "next"->service.next(championshipId,auctionId);default->throw new IllegalArgumentException("Unknown action");};
    }
    @PostMapping("/admin/set-current") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public AuctionStateDto setCurrent(@PathVariable UUID championshipId,@PathVariable UUID auctionId,@Valid @RequestBody NextPlayerRequest req){return service.setCurrent(championshipId,auctionId,req.playerId());}
    @PostMapping("/admin/reset-unsold") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public AuctionStateDto resetUnsold(@PathVariable UUID championshipId,@PathVariable UUID auctionId){return service.resetUnsold(championshipId,auctionId);}
    @PostMapping("/admin/set-status") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public AuctionStateDto setStatus(@PathVariable UUID championshipId,@PathVariable UUID auctionId,@Valid @RequestBody SetStatusRequest req){return service.status(championshipId,auctionId,req.status());}
    @PostMapping("/admin/coin-toss") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public Map<String,Object> toss(@PathVariable UUID championshipId,@PathVariable UUID auctionId,@RequestBody Map<String,List<UUID>> req){return service.coinToss(championshipId,auctionId,req.get("teamIds"));}
    @PutMapping("/admin/players/{playerId}/base-price") @PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
    public PlayerDto price(@PathVariable UUID championshipId,@PathVariable UUID auctionId,@PathVariable UUID playerId,@Valid @RequestBody UpdateBasePriceRequest req){return service.basePrice(championshipId,auctionId,playerId,req.basePrice());}
}
