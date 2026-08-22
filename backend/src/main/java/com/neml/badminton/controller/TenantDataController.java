package com.neml.badminton.controller;

import com.neml.badminton.dto.Dtos.*;
import com.neml.badminton.dto.MatchDtos.MatchDto;
import com.neml.badminton.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/championships/{championshipId}")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
public class TenantDataController {
    private final TeamRepository teams; private final PlayerRepository players; private final MatchRepository matches;
    public TenantDataController(TeamRepository teams,PlayerRepository players,MatchRepository matches){this.teams=teams;this.players=players;this.matches=matches;}
    @GetMapping("/teams") public List<TeamDto> teams(@PathVariable UUID championshipId){return teams.findAllByChampionshipIdOrderByName(championshipId).stream().map(TeamDto::from).toList();}
    @GetMapping("/teams/{id}") public TeamDto team(@PathVariable UUID championshipId,@PathVariable UUID id){return teams.findByIdAndChampionshipId(id,championshipId).map(TeamDto::from).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
    @GetMapping("/teams/{id}/players") public List<PlayerDto> teamPlayers(@PathVariable UUID championshipId,@PathVariable UUID id){
        teams.findByIdAndChampionshipId(id,championshipId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        return players.findAllByChampionshipIdOrderByAuctionOrderAsc(championshipId).stream().filter(p->p.getTeam()!=null&&p.getTeam().getId().equals(id)).map(PlayerDto::from).toList();}
    @GetMapping("/players") public List<PlayerDto> players(@PathVariable UUID championshipId){return players.findAllByChampionshipIdOrderByAuctionOrderAsc(championshipId).stream().map(PlayerDto::from).toList();}
    @GetMapping("/players/{id}") public PlayerDto player(@PathVariable UUID championshipId,@PathVariable UUID id){return players.findByIdAndChampionshipId(id,championshipId).map(PlayerDto::from).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
    @GetMapping("/matches") public List<MatchDto> matches(@PathVariable UUID championshipId){return matches.findAllByChampionshipIdOrderByMatchNumberAsc(championshipId).stream().map(MatchDto::from).toList();}
    @GetMapping("/matches/{id}") public MatchDto match(@PathVariable UUID championshipId,@PathVariable UUID id){return matches.findByIdAndChampionshipId(id,championshipId).map(MatchDto::from).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
}
