package com.neml.badminton.certificate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neml.badminton.competition.SportEngineRegistry;
import com.neml.badminton.certificate.CertificateModels.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
public class CertificateEligibilityService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final SportEngineRegistry engines;
    public CertificateEligibilityService(JdbcTemplate db,ObjectMapper json,SportEngineRegistry engines) {
        this.db=db; this.json=json; this.engines=engines;
    }
    public void lock(UUID cid) {
        if(db.queryForList("select id from play_neml.championships where id=? for update",cid).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Championship not found");
    }
    public Optional<Approval> active(UUID cid) {
        return db.query("select snapshot from play_neml.certificate_approvals where championship_id=? and invalidated_at is null",
                (rs,n)->read(rs.getString(1),Approval.class),cid).stream().findFirst();
    }
    @Transactional
    public Approval approve(UUID cid,UUID actor,String justification) {
        if(justification==null||justification.trim().length()<10||justification.length()>1000)throw conflict("Provide a result approval justification of 10 to 1000 characters");
        lock(cid);
        var source=current(cid,true);
        var existing=active(cid);
        if(existing.isPresent()) {
            assertCurrent(existing.get());
            return existing.get();
        }
        String name=db.queryForObject("select full_name from play_neml.users where id=?",String.class,actor);
        var approval=new Approval(UUID.randomUUID(),cid,source.championship(),source.sport(),source.champion(),
                source.runnerUp(),source.sources(),source.participants(),Instant.now(),name,justification.trim());
        db.update("insert into play_neml.certificate_approvals(id,championship_id,snapshot,approved_by,approved_at) values(?,?,?,?,?)",
                approval.id(),cid,write(approval),actor,java.sql.Timestamp.from(approval.approvedAt()));
        audit(cid,null,approval.id(),actor,"RESULT_APPROVED",justification.trim());
        return approval;
    }
    @Transactional(readOnly=true)
    public Eligibility eligibility(UUID cid,Category category,RecipientType type) {
        var approval=active(cid);
        if(approval.isEmpty()) return new Eligibility(null,List.of(),
                "Complete all fixtures and the final, then approve the results. Participation requires a completed rubber lineup (badminton) or locked playing XI in a played, completed match (cricket).");
        assertCurrent(approval.get());
        return new Eligibility(approval.get(),eligible(approval.get(),category,type),
                category==Category.PARTICIPATION?"Completed-match participation only. Registration and an unused roster place do not qualify.":"Team award established by the approved championship final.");
    }
    public List<Recipient> eligible(Approval a,Category category,RecipientType type) {
        if(category!=Category.PARTICIPATION && type!=RecipientType.TEAM) return List.of();
        return a.participants().stream().filter(p->p.type()==type)
                .filter(p->category==Category.PARTICIPATION || p.id().equals(category==Category.WINNER?a.champion():a.runnerUp())).toList();
    }
    public Approval require(UUID cid,UUID id) {
        var a=active(cid).filter(x->x.id().equals(id)).orElseThrow(()->conflict("Approval is missing or outdated; refresh eligibility"));
        assertCurrent(a,true); return a;
    }
    /** Identity approval survives cosmetic edits; each new issuance snapshots the current authorized name. */
    public Recipient currentRecipient(UUID cid,Recipient approved) {
        String sql=approved.type()==RecipientType.TEAM?
                "select name,null::text as team_name,null::uuid as photo_key from play_neml.teams where championship_id=? and id=?":
                "select p.full_name,t.name,p.photo_key from play_neml.players p left join play_neml.teams t on t.id=p.team_id and t.championship_id=p.championship_id where p.championship_id=? and p.id=?";
        return db.query(sql,(rs,n)->new Recipient(approved.type(),approved.id(),rs.getString(1),rs.getString(2),
                rs.getObject(3)==null?null:"/api/championships/"+cid+"/players/"+approved.id()+"/photo?version="+rs.getObject(3)),cid,approved.id())
                .stream().findFirst().orElseThrow(()->conflict("Approved recipient no longer belongs to the championship"));
    }
    public void assertCurrent(Approval a) {
        assertCurrent(a,false);
    }
    private void assertCurrent(Approval a,boolean locking) {
        var now=current(a.championshipId(),locking);
        if(!now.sources().equals(a.sources()) || !now.champion().equals(a.champion()) || !now.runnerUp().equals(a.runnerUp())
                || !now.participants().stream().map(p->p.type()+":"+p.id()).toList().equals(a.participants().stream().map(p->p.type()+":"+p.id()).toList()))
            throw conflict("Source result or participation revision changed; approve results again");
    }
    private Approval current(UUID cid,boolean locking) {
        var rows=db.queryForList("select c.name,c.sport_type,c.lifecycle_phase,cc.champion_team_id,cc.runner_up_team_id from play_neml.championships c join play_neml.competition_configurations cc on cc.championship_id=c.id where c.id=?",cid);
        if(rows.size()!=1) throw conflict("Competition configuration is unavailable");
        var c=rows.get(0);String sport=engines.require(c.get("sport_type").toString()).sportCode();
        if(!"COMPLETED".equals(c.get("lifecycle_phase"))) throw conflict("Complete the championship final before approving certificates");
        var fixtures=db.queryForList("select id,status,stage_type,result_revision,winner_team_id,team_a_id,team_b_id from play_neml.matches where championship_id=? order by id"+(locking?" for update":""),cid);
        if(fixtures.isEmpty() || fixtures.stream().anyMatch(m->!"COMPLETED".equals(m.get("status"))))
            throw conflict("Every competition fixture must be completed before certificate approval");
        var finals=fixtures.stream().filter(m->"FINAL".equals(m.get("stage_type"))).toList();
        if(finals.size()!=1 || finals.get(0).get("winner_team_id")==null) throw conflict("A decisive completed final is required");
        var f=finals.get(0);UUID champion=(UUID)f.get("winner_team_id");
        if(!champion.equals(f.get("team_a_id"))&&!champion.equals(f.get("team_b_id"))) throw conflict("Invalid final winner");
        UUID runner=(UUID)f.get(champion.equals(f.get("team_a_id"))?"team_b_id":"team_a_id");
        if(db.queryForObject("select count(*) from play_neml.teams where championship_id=? and id in (?,?)",Long.class,cid,champion,runner)!=2)
            throw conflict("Final recipients must belong to the championship");
        if(champion.equals(runner)||!champion.equals(c.get("champion_team_id"))||!runner.equals(c.get("runner_up_team_id")))
            throw conflict("Final and authoritative championship results disagree");
        if(db.queryForObject("select count(*) from play_neml.competition_stages where championship_id=? and stage_type='FINAL' and status='COMPLETED'",Long.class,cid)!=1)
            throw conflict("Final stage has not been finalized");
        List<Source> sources=fixtures.stream().map(m->new Source((UUID)m.get("id"),((Number)m.get("result_revision")).intValue(),m.get("stage_type").toString(),(UUID)m.get("winner_team_id"),(UUID)m.get("team_a_id"),(UUID)m.get("team_b_id"))).toList();
        List<Recipient> recipients=new ArrayList<>(db.query("""
            select t.id,t.name from play_neml.teams t where t.championship_id=? and exists
            (select 1 from play_neml.matches m where m.championship_id=t.championship_id and m.status='COMPLETED' and t.id in (m.team_a_id,m.team_b_id)) order by t.id
            """,(rs,n)->new Recipient(RecipientType.TEAM,rs.getObject(1,UUID.class),rs.getString(2),null,null),cid));
        String participation=switch(sport) {
            case "BADMINTON" -> """
                select fa.player_id from play_neml.format_side_a_players fa join play_neml.match_formats mf on mf.id=fa.format_id
                join play_neml.matches m on m.id=mf.match_id and m.championship_id=mf.championship_id
                where mf.championship_id=? and mf.completed=true and m.status='COMPLETED'
                union select fb.player_id from play_neml.format_side_b_players fb join play_neml.match_formats mf on mf.id=fb.format_id
                join play_neml.matches m on m.id=mf.match_id and m.championship_id=mf.championship_id
                where mf.championship_id=? and mf.completed=true and m.status='COMPLETED'
                """;
            case "CRICKET" -> """
                select cm.player_id from play_neml.cricket_playing_members cm join play_neml.matches m on m.id=cm.match_id and m.championship_id=cm.championship_id
                where cm.championship_id=? and m.status='COMPLETED' and exists
                (select 1 from play_neml.cricket_innings i where i.match_id=m.id and i.championship_id=? and i.status='COMPLETED')
                """;
            default -> throw conflict("This sport does not yet support certificate participation");
        };
        recipients.addAll(db.query("select p.id,p.full_name,t.name,p.photo_key from play_neml.players p left join play_neml.teams t on t.id=p.team_id and t.championship_id=p.championship_id where p.championship_id=? and p.id in ("+participation+") order by p.id",
                (rs,n)->new Recipient(RecipientType.PLAYER,rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),
                        rs.getObject(4)==null?null:"/api/championships/"+cid+"/players/"+rs.getObject(1)+"/photo?version="+rs.getObject(4)),cid,cid,cid));
        return new Approval(null,cid,c.get("name").toString(),sport,champion,runner,sources,recipients,null,null,null);
    }
    public void audit(UUID cid,UUID certificate,UUID approval,UUID actor,String action,String reason) {
        db.update("insert into play_neml.certificate_audit(id,championship_id,certificate_id,approval_id,actor_id,action,reason) values(?,?,?,?,?,?,?)",UUID.randomUUID(),cid,certificate,approval,actor,action,reason);
    }
    public String write(Object value) {try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Unable to persist certificate snapshot",e);}}
    public <T>T read(String value,Class<T> type) {try{return json.readValue(value,type);}catch(Exception e){throw new IllegalStateException("Unable to read certificate snapshot",e);}}
    static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
