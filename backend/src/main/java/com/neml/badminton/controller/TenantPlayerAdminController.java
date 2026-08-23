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

    public TenantPlayerAdminController(ChampionshipRepository championships, AuctionRepository auctions,
            PlayerRepository players, TournamentSettingsRepository settings, AuctionStateRepository states,
            BidRepository bids, org.springframework.jdbc.core.JdbcTemplate jdbc,
            SquadConfirmationService squadConfirmations) {
        this.championships=championships; this.auctions=auctions; this.players=players; this.settings=settings;
        this.states=states; this.bids=bids; this.jdbc=jdbc;
        this.squadConfirmations=squadConfirmations;
    }

    public record PlayerRequest(@NotBlank String fullName, @NotNull Gender gender,
                                @DecimalMin("0.01") BigDecimal basePrice,
                                String skillLevel, @Min(1) Integer auctionOrder) {}

    @PostMapping @Transactional
    public PlayerDto create(@PathVariable UUID championshipId, @Valid @RequestBody PlayerRequest req) {
        squadConfirmations.assertRosterMutable(championshipId);
        Championship championship=championships.findByIdForUpdate(championshipId).orElseThrow(()->notFound("Championship"));
        Auction auction=auctions.findAllByChampionshipIdOrderByCreatedAtDesc(championshipId).stream().findFirst()
                .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Configure the championship before adding players"));
        TournamentSettings config=settings.findByChampionshipId(championshipId).orElseThrow(()->notFound("Settings"));
        int next=players.findAllByChampionshipIdOrderByAuctionOrderAsc(championshipId).stream()
                .mapToInt(p->p.getAuctionOrder()==null?0:p.getAuctionOrder()).max().orElse(0)+1;
        Player player=Player.builder().championship(championship).auction(auction).fullName(req.fullName().trim())
                .gender(req.gender()).basePrice(req.basePrice()==null?config.getPlayerBasePrice():req.basePrice())
                .skillLevel(req.skillLevel()).status(PlayerStatus.AVAILABLE)
                .auctionOrder(req.auctionOrder()==null?next:req.auctionOrder()).build();
        return PlayerDto.from(players.save(player));
    }

    @PutMapping("/{id}") @Transactional
    public PlayerDto update(@PathVariable UUID championshipId,@PathVariable UUID id,@Valid @RequestBody PlayerRequest req){
        squadConfirmations.assertRosterMutable(championshipId);
        Player player=players.findByIdAndChampionshipId(id,championshipId).orElseThrow(()->notFound("Player"));
        if(player.getStatus()==PlayerStatus.SOLD)throw new ResponseStatusException(HttpStatus.CONFLICT,"Sold players cannot be edited");
        player.setFullName(req.fullName().trim());player.setGender(req.gender());
        if(req.basePrice()!=null)player.setBasePrice(req.basePrice());player.setSkillLevel(req.skillLevel());
        if(req.auctionOrder()!=null)player.setAuctionOrder(req.auctionOrder());return PlayerDto.from(players.save(player));
    }

    @DeleteMapping("/{id}") @Transactional
    public void delete(@PathVariable UUID championshipId,@PathVariable UUID id){
        squadConfirmations.assertRosterMutable(championshipId);
        Player player=players.findByIdAndChampionshipId(id,championshipId).orElseThrow(()->notFound("Player"));
        if(player.getStatus()==PlayerStatus.SOLD)throw new ResponseStatusException(HttpStatus.CONFLICT,"Sold players cannot be deleted");
        states.findAllByChampionshipId(championshipId).stream()
                .filter(s->s.getCurrentPlayer()!=null&&s.getCurrentPlayer().getId().equals(id))
                .forEach(s->{s.setCurrentPlayer(null);states.save(s);});
        jdbc.update("DELETE FROM play_neml.format_side_a_players WHERE player_id = ?",id);
        jdbc.update("DELETE FROM play_neml.format_side_b_players WHERE player_id = ?",id);
        bids.deleteAll(bids.findAllByPlayerAndChampionshipIdOrderByCreatedAtDesc(player,championshipId));players.delete(player);
    }

    private ResponseStatusException notFound(String value){return new ResponseStatusException(HttpStatus.NOT_FOUND,value+" not found");}
}
