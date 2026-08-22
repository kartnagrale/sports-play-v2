package com.neml.badminton.controller;

import com.neml.badminton.dto.Dtos.PlayerDto;
import com.neml.badminton.entity.Gender;
import com.neml.badminton.entity.Player;
import com.neml.badminton.entity.PlayerStatus;
import com.neml.badminton.repository.PlayerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/players")
public class AdminManagementController {

    private final PlayerRepository playerRepository;
    private final com.neml.badminton.repository.BidRepository bidRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.neml.badminton.repository.TeamRepository teamRepository;
    private final com.neml.badminton.repository.AuctionStateRepository auctionStateRepository;

    public AdminManagementController(PlayerRepository playerRepository, 
                                     com.neml.badminton.repository.BidRepository bidRepository,
                                     org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
                                     com.neml.badminton.repository.TeamRepository teamRepository,
                                     com.neml.badminton.repository.AuctionStateRepository auctionStateRepository) {
        this.playerRepository = playerRepository;
        this.bidRepository = bidRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.teamRepository = teamRepository;
        this.auctionStateRepository = auctionStateRepository;
    }

    public static class PlayerRequest {
        public String fullName;
        public String gender;
        public BigDecimal basePrice;
        public String skillLevel;
        public Integer auctionOrder;
    }

    @PostMapping
    public ResponseEntity<PlayerDto> createPlayer(@RequestBody PlayerRequest req) {
        int maxOrder = playerRepository.findAll().stream()
                .mapToInt(p -> p.getAuctionOrder() != null ? p.getAuctionOrder() : 0)
                .max().orElse(0);

        Player p = Player.builder()
                .fullName(req.fullName)
                .gender(Gender.valueOf(req.gender))
                .basePrice(req.basePrice != null ? req.basePrice : BigDecimal.ZERO)
                .skillLevel(req.skillLevel)
                .status(PlayerStatus.AVAILABLE)
                .auctionOrder(req.auctionOrder != null ? req.auctionOrder : maxOrder + 1)
                .build();
        return ResponseEntity.ok(PlayerDto.from(playerRepository.save(p)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerDto> updatePlayer(@PathVariable UUID id, @RequestBody PlayerRequest req) {
        Player p = playerRepository.findById(id).orElseThrow();
        p.setFullName(req.fullName);
        p.setGender(Gender.valueOf(req.gender));
        p.setBasePrice(req.basePrice);
        p.setSkillLevel(req.skillLevel);
        if (req.auctionOrder != null) {
            p.setAuctionOrder(req.auctionOrder);
        }
        return ResponseEntity.ok(PlayerDto.from(playerRepository.save(p)));
    }

    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> deletePlayer(@PathVariable UUID id) {
        Player p = playerRepository.findById(id).orElseThrow();
        
        auctionStateRepository.findAll().forEach(state -> {
            if (state.getCurrentPlayer() != null && state.getCurrentPlayer().getId().equals(p.getId())) {
                state.setCurrentPlayer(null);
                auctionStateRepository.save(state);
            }
        });

        jdbcTemplate.update("DELETE FROM format_side_a_players WHERE player_id = ?", p.getId());
        jdbcTemplate.update("DELETE FROM format_side_b_players WHERE player_id = ?", p.getId());
        bidRepository.deleteAll(bidRepository.findAllByPlayerOrderByCreatedAtDesc(p));
        playerRepository.delete(p);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/reset-all")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> resetAll() {
        jdbcTemplate.update("UPDATE auction_state SET current_player_id = NULL");
        jdbcTemplate.update("DELETE FROM format_side_a_players");
        jdbcTemplate.update("DELETE FROM format_side_b_players");
        jdbcTemplate.update("DELETE FROM match_formats");
        jdbcTemplate.update("DELETE FROM matches");
        jdbcTemplate.update("DELETE FROM bids");
        jdbcTemplate.update("DELETE FROM players");
        
        teamRepository.findAll().forEach(team -> {
            team.setPurseRemaining(team.getPurseTotal());
            team.setMaleCount(0);
            team.setFemaleCount(0);
            team.setMatchPoints(0);
            teamRepository.save(team);
        });

        auctionStateRepository.findAll().forEach(state -> {
            state.setStatus(com.neml.badminton.entity.AuctionStatus.NOT_STARTED);
            state.setCurrentPlayer(null);
            state.setTimerSeconds(30);
            state.setBidDeadline(null);
            auctionStateRepository.save(state);
        });
        
        return ResponseEntity.ok().build();
    }
}
