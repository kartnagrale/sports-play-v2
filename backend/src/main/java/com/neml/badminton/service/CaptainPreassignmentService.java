package com.neml.badminton.service;

import com.neml.badminton.dto.Dtos.PlayerDto;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
public class CaptainPreassignmentService {
    public record Request(UUID playerId,@NotNull Gender gender,@NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9_-]{0,63}") String playerReference) {}
    private final ChampionshipRepository championships;
    private final ChampionshipRoleRepository roles;
    private final PlayerRepository players;
    private final TeamRepository teams;
    private final TournamentSettingsRepository settings;
    private final AuctionPolicyGuard guard;
    private final SquadConfirmationService squads;
    private final CompetitionAuditEventRepository audit;
    public CaptainPreassignmentService(ChampionshipRepository championships,ChampionshipRoleRepository roles,
            PlayerRepository players,TeamRepository teams,TournamentSettingsRepository settings,
            AuctionPolicyGuard guard,SquadConfirmationService squads,CompetitionAuditEventRepository audit) {
        this.championships=championships;this.roles=roles;this.players=players;this.teams=teams;this.settings=settings;
        this.guard=guard;this.squads=squads;this.audit=audit;
    }
    public record Assignment(UUID captainUserId,UUID teamId,String captainName,UUID playerId,Gender gender,String playerReference) {}
    @Transactional public List<Assignment> assignments(UUID cid) {
        List<Player> all=players.findAllByChampionshipIdOrderByAuctionOrderAsc(cid);
        return roles.findAllByChampionshipId(cid).stream().filter(r->r.getRole()==ChampionshipRoleType.TEAM_CAPTAIN&&r.getTeam()!=null).map(r->{
            Player p=all.stream().filter(x->r.getUser().getId().equals(x.getCaptainUserId())).findFirst().orElse(null);
            return new Assignment(r.getUser().getId(),r.getTeam().getId(),r.getUser().getFullName(),p==null?null:p.getId(),p==null?null:p.getGender(),p==null?null:p.getPlayerReference());
        }).toList();
    }
    @Transactional public PlayerDto assign(UUID cid,UUID teamId,UUID captainId,Request request,User actor) {
        Championship c=championships.findByIdForUpdate(cid).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Championship not found"));
        ChampionshipRole role=roles.findByUserIdAndChampionshipId(captainId,cid).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Captain membership not found"));
        if(role.getRole()!=ChampionshipRoleType.TEAM_CAPTAIN||role.getTeam()==null||!role.getTeam().getId().equals(teamId))
            throw error(HttpStatus.CONFLICT,"Captain login must belong to this team and championship");
        String reference=PlayerRegistrationService.reference(request.playerReference());
        Player assigned=players.findByChampionshipIdAndCaptainUserId(cid,captainId).orElse(null);
        if(assigned!=null) {
            if(!assigned.getTeam().getId().equals(teamId)||(request.playerId()!=null&&!request.playerId().equals(assigned.getId()))
                    ||assigned.getGender()!=request.gender()||!reference.equals(assigned.getPlayerReference()))
                throw error(HttpStatus.CONFLICT,"Captain already has a different player assignment");
            return PlayerDto.from(assigned);
        }
        guard.lockEditable(cid);squads.assertRosterMutable(cid);
        List<Player> all=players.findAllByChampionshipIdOrderByAuctionOrderAsc(cid);
        if(all.stream().anyMatch(p->p.getStatus()==PlayerStatus.PREASSIGNED&&p.getTeam().getId().equals(teamId)))
            throw error(HttpStatus.CONFLICT,"Team already has a preassigned captain");
        Player p=request.playerId()==null?all.stream().filter(x->reference.equals(x.getPlayerReference())).findFirst().orElse(null)
                :players.findByIdAndChampionshipId(request.playerId(),cid).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Player not found in this championship"));
        if(p!=null&&(p.getTeam()!=null||p.getCaptainUserId()!=null||p.getStatus()!=PlayerStatus.AVAILABLE||p.getGender()!=request.gender()))
            throw error(HttpStatus.CONFLICT,"Choose an unassigned available player with the correct gender");
        UUID chosenId=p==null?null:p.getId();
        if(all.stream().anyMatch(x->reference.equals(x.getPlayerReference())&&!Objects.equals(x.getId(),chosenId)))
            throw error(HttpStatus.CONFLICT,"Player Reference belongs to another identity");
        if(p!=null&&p.getPlayerReference()!=null&&!reference.equals(p.getPlayerReference()))
            throw error(HttpStatus.CONFLICT,"Use the selected player's existing Player Reference");
        TournamentSettings cfg=settings.findByChampionshipId(cid).orElseThrow();
        List<Player> roster=all.stream().filter(x->x.getTeam()!=null&&x.getTeam().getId().equals(teamId)).toList();
        if(roster.size()>=cfg.getMaxSquadSize())throw error(HttpStatus.CONFLICT,"Team squad is full");
        Team team=role.getTeam();
        if(p==null)p=Player.builder().championship(c).fullName(role.getUser().getFullName()).gender(request.gender()).basePrice(cfg.getPlayerBasePrice()).build();
        p.setCaptainUserId(captainId);p.setPlayerReference(reference);p.setTeam(team);p.setAuction(null);p.setAuctionOrder(null);
        p.setStatus(PlayerStatus.PREASSIGNED);p.setSoldPrice(null);players.saveAndFlush(p);
        team.setMaleCount((int)roster.stream().filter(x->x.getGender()==Gender.MALE).count()+(p.getGender()==Gender.MALE?1:0));
        team.setFemaleCount((int)roster.stream().filter(x->x.getGender()==Gender.FEMALE).count()+(p.getGender()==Gender.FEMALE?1:0));
        teams.save(team);
        audit.save(CompetitionAuditEvent.builder().championship(c).actor(actor).eventType("CAPTAIN_PLAYER_PREASSIGNED")
                .revision(1).entityType("PLAYER").entityId(p.getId()).correlationId(UUID.randomUUID())
                .payload("{\"teamId\":\""+teamId+"\",\"captainUserId\":\""+captainId+"\"}").occurredAt(Instant.now()).build());
        return PlayerDto.from(p);
    }
    private ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
