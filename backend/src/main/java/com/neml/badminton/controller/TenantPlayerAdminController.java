package com.neml.badminton.controller;

import com.neml.badminton.dto.Dtos.PlayerDto;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.neml.badminton.service.SquadConfirmationService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/championships/{championshipId}/admin/players")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
public class TenantPlayerAdminController {
    private final ChampionshipRepository championships; private final AuctionRepository auctions;
    private final PlayerRepository players; private final TournamentSettingsRepository settings;
    private final AuctionStateRepository states; private final BidRepository bids;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final SquadConfirmationService squadConfirmations;
    private final com.neml.badminton.service.PlayerRegistrationService registration;
    private final com.neml.badminton.service.PlayerPhotoService photos;

    public TenantPlayerAdminController(ChampionshipRepository championships, AuctionRepository auctions,
            PlayerRepository players, TournamentSettingsRepository settings, AuctionStateRepository states,
            BidRepository bids, org.springframework.jdbc.core.JdbcTemplate jdbc,
            SquadConfirmationService squadConfirmations, com.neml.badminton.service.PlayerRegistrationService registration,com.neml.badminton.service.PlayerPhotoService photos) {
        this.championships=championships; this.auctions=auctions; this.players=players; this.settings=settings;
        this.states=states; this.bids=bids; this.jdbc=jdbc;
        this.squadConfirmations=squadConfirmations;
        this.registration=registration;
        this.photos=photos;
    }

    public record PlayerRequest(@NotBlank @Size(max=255) String fullName, @NotNull Gender gender,
                                @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal basePrice,
                                @Size(max=255) String skillLevel, @Min(1) Integer auctionOrder, @Size(max=64) String playerReference) {}

    @PostMapping @Transactional
    public PlayerDto create(@PathVariable UUID championshipId, @Valid @RequestBody PlayerRequest req) {
        return PlayerDto.from(registration.register(championshipId,java.util.List.of(new com.neml.badminton.service.PlayerRegistrationService.Registration(
                req.fullName(),req.gender(),req.basePrice(),req.skillLevel(),req.auctionOrder(),req.playerReference()))).get(0));
    }

    @PutMapping("/{id}") @Transactional
    public PlayerDto update(@PathVariable UUID championshipId,@PathVariable UUID id,@Valid @RequestBody PlayerRequest req){
        championships.findByIdForUpdate(championshipId).orElseThrow(()->notFound("Championship"));
        squadConfirmations.assertRosterMutable(championshipId);
        Player player=players.findByIdAndChampionshipId(id,championshipId).orElseThrow(()->notFound("Player"));
        if(player.getStatus()==PlayerStatus.SOLD)throw new ResponseStatusException(HttpStatus.CONFLICT,"Sold players cannot be edited");
        String reference=com.neml.badminton.service.PlayerRegistrationService.reference(req.playerReference());
        if(reference!=null){
            var errors=com.neml.badminton.service.PlayerRegistrationService.validate(new com.neml.badminton.service.PlayerRegistrationService.Registration(req.fullName(),req.gender(),req.basePrice(),req.skillLevel(),req.auctionOrder(),reference));
            if(!errors.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,String.join("; ",errors));
            if(player.getPlayerReference()!=null&&!reference.equals(player.getPlayerReference()))throw new ResponseStatusException(HttpStatus.CONFLICT,"A stable Player Reference cannot be changed");
            player.setPlayerReference(reference);
        }
        player.setFullName(req.fullName().trim());player.setGender(req.gender());
        if(req.basePrice()!=null)player.setBasePrice(req.basePrice());player.setSkillLevel(req.skillLevel());
        if(req.auctionOrder()!=null)player.setAuctionOrder(req.auctionOrder());return PlayerDto.from(players.save(player));
    }

    @DeleteMapping("/{id}") @Transactional
    public void delete(@PathVariable UUID championshipId,@PathVariable UUID id,@org.springframework.security.core.annotation.AuthenticationPrincipal User actor){
        championships.findByIdForUpdate(championshipId).orElseThrow(()->notFound("Championship"));
        squadConfirmations.assertRosterMutable(championshipId);
        Player player=players.findByIdAndChampionshipId(id,championshipId).orElseThrow(()->notFound("Player"));
        if(player.getStatus()==PlayerStatus.SOLD)throw new ResponseStatusException(HttpStatus.CONFLICT,"Sold players cannot be deleted");
        if(player.getPhotoKey()!=null)photos.remove(championshipId,id,actor.getId());
        states.findAllByChampionshipId(championshipId).stream()
                .filter(s->s.getCurrentPlayer()!=null&&s.getCurrentPlayer().getId().equals(id))
                .forEach(s->{s.setCurrentPlayer(null);states.save(s);});
        jdbc.update("DELETE FROM play_neml.format_side_a_players WHERE player_id = ?",id);
        jdbc.update("DELETE FROM play_neml.format_side_b_players WHERE player_id = ?",id);
        bids.deleteAll(bids.findAllByPlayerAndChampionshipIdOrderByCreatedAtDesc(player,championshipId));players.delete(player);
    }

    private ResponseStatusException notFound(String value){return new ResponseStatusException(HttpStatus.NOT_FOUND,value+" not found");}
}
