package com.neml.badminton.service;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.ChampionshipRoleRepository;
import com.neml.badminton.security.*;
import com.neml.badminton.dto.Dtos.TeamDto;
import com.neml.badminton.dto.MatchDtos.MatchDto;
import com.neml.badminton.repository.TeamRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Aggregate counts rather than loading participant tables into dashboard clients. */
@Service
public class DashboardAnalyticsService {
    public record Purse(UUID id,String name,BigDecimal allocated,BigDecimal remaining) {}
    public record Activity(String championship,String action,Instant at) {}
    public record Platform(long championships,long participants,Map<String,Long> status,Map<String,Long> sports,
            Map<String,Long> lifecycle,List<Activity> recent) {}
    public record Captain(TeamDto team,Map<String,Long> composition,long played,long won,long lost,String squadStatus,
            List<MatchDto> upcoming,List<MatchDto> recent) {}
    public record Summary(Map<String,Long> matches,Map<String,Long> players,Map<String,Long> certificates,
            List<Purse> purses,Map<String,Long> squads,Captain captain,Instant asOf) {}
    private final JdbcTemplate db;private final ChampionshipSecurity security;private final ChampionshipRoleRepository roles;
    private final TeamRepository teams;private final MatchService matches;
    public DashboardAnalyticsService(JdbcTemplate db,ChampionshipSecurity security,ChampionshipRoleRepository roles,TeamRepository teams,MatchService matches){this.db=db;this.security=security;this.roles=roles;this.teams=teams;this.matches=matches;}
    @Transactional(readOnly=true)
    public Platform platform() {
        var recent=db.query("select c.name,e.event_type,e.occurred_at from play_neml.competition_audit_events e join play_neml.championships c on c.id=e.championship_id order by e.occurred_at desc,e.id limit 5",
                (rs,n)->new Activity(rs.getString(1),rs.getString(2),rs.getTimestamp(3).toInstant()));
        return new Platform(count("select count(*) from play_neml.championships"),count("select count(*) from play_neml.players"),
                grouped("select status,count(*) from play_neml.championships group by status"),grouped("select sport_type,count(*) from play_neml.championships group by sport_type"),
                grouped("select lifecycle_phase,count(*) from play_neml.championships group by lifecycle_phase"),recent);
    }
    @Transactional(readOnly=true)
    public Summary summary(UUID cid,Authentication auth) {
        var matchCounts=grouped("select status,count(*) from play_neml.matches where championship_id=? group by status",cid);
        if(security.canManage(cid,auth)) {
            var purses=db.query("select id,name,purse_total,purse_remaining from play_neml.teams where championship_id=? order by name limit 100",(rs,n)->new Purse(rs.getObject(1,UUID.class),rs.getString(2),rs.getBigDecimal(3),rs.getBigDecimal(4)),cid);
            return new Summary(matchCounts,grouped("select status,count(*) from play_neml.players where championship_id=? group by status",cid),
                    grouped("select status,count(*) from play_neml.certificates where championship_id=? group by status",cid),purses,
                    grouped("select coalesce(s.status,'DRAFT'),count(*) from play_neml.teams t left join play_neml.squad_confirmations s on s.team_id=t.id and s.championship_id=t.championship_id where t.championship_id=? group by coalesce(s.status,'DRAFT')",cid),null,Instant.now());
        }
        Captain captain=null;
        if(auth.getPrincipal() instanceof User user && user.getRole()!=Role.SUPER_ADMIN) {
            var role=roles.findByUserIdAndChampionshipId(user.getId(),cid).orElse(null);
            if(role!=null&&role.getRole()==ChampionshipRoleType.TEAM_CAPTAIN&&role.getTeam()!=null) {
                UUID team=role.getTeam().getId();Team entity=teams.findByIdAndChampionshipId(team,cid).orElseThrow();
                long played=count("select count(*) from play_neml.matches where championship_id=? and status='COMPLETED' and ? in(team_a_id,team_b_id)",cid,team);
                long won=count("select count(*) from play_neml.matches where championship_id=? and status='COMPLETED' and winner_team_id=?",cid,team);
                long lost=count("select count(*) from play_neml.matches where championship_id=? and status='COMPLETED' and ? in(team_a_id,team_b_id) and winner_team_id is not null and winner_team_id<>?",cid,team,team);
                var status=db.queryForList("select status from play_neml.squad_confirmations where championship_id=? and team_id=?",String.class,cid,team);
                captain=new Captain(TeamDto.from(entity),grouped("select gender,count(*) from play_neml.players where championship_id=? and team_id=? group by gender",cid,team),played,won,lost,status.isEmpty()?"DRAFT":status.get(0),fixtures(cid,team,false),fixtures(cid,team,true));
            }
        }
        // Spectators and platform observers receive competition progress only, never private operations/certificates.
        return new Summary(matchCounts,null,null,null,null,captain,Instant.now());
    }
    private List<MatchDto> fixtures(UUID cid,UUID team,boolean completed){return db.queryForList("select id from play_neml.matches where championship_id=? and ? in(team_a_id,team_b_id) and status"+(completed?"=":"<>")+"'COMPLETED' order by scheduled_at "+(completed?"desc":"asc")+",id limit 5",UUID.class,cid,team).stream().map(id->matches.get(cid,id)).toList();}
    private long count(String sql,Object... args){return db.queryForObject(sql,Long.class,args);}
    private Map<String,Long> grouped(String sql,Object... args){Map<String,Long> result=new TreeMap<>();db.query(sql,rs->{result.put(rs.getString(1),rs.getLong(2));},args);return result;}
}
