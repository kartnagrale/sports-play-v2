package com.neml.badminton.certificate;

import com.neml.badminton.certificate.CertificateModels.*;
import com.neml.badminton.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static com.neml.badminton.certificate.CertificateEligibilityService.conflict;

@Service
public class CertificateService {
    private final JdbcTemplate db;
    private final CertificateEligibilityService eligibility;
    private final CertificatePdfRenderer renderer;
    private final CertificateVerificationPolicy verification;
    public CertificateService(JdbcTemplate db,CertificateEligibilityService eligibility,CertificatePdfRenderer renderer,CertificateVerificationPolicy verification) {
        this.db=db;this.eligibility=eligibility;this.renderer=renderer;this.verification=verification;
    }
    @Transactional
    public Issued issue(UUID cid,User actor,IssueRequest request) {
        eligibility.lock(cid);
        var previous=db.queryForList("select id,issued_by,request_payload from play_neml.certificates where championship_id=? and request_id=?",cid,request.requestId());
        if(!previous.isEmpty()) {
            if(!actor.getId().equals(previous.get(0).get("issued_by"))||!eligibility.write(request).equals(previous.get(0).get("request_payload")))
                throw conflict("Idempotency key was already used for a different request");
            return get(cid,(UUID)previous.get(0).get("id"));
        }
        var snapshot=prepare(cid,actor,request,false);
        // Validate actual font coverage and renderability before committing an issuance.
        renderer.render(snapshot,false);
        db.update("""
            insert into play_neml.certificates(id,public_reference,championship_id,approval_id,category,recipient_type,recipient_id,team_id,player_id,snapshot,issued_by,issued_at,request_id,request_payload,reissued_from)
            values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,snapshot.id(),snapshot.reference(),cid,snapshot.approvalId(),snapshot.category().name(),snapshot.recipient().type().name(),snapshot.recipient().id(),
                snapshot.recipient().type()==RecipientType.TEAM?snapshot.recipient().id():null,
                snapshot.recipient().type()==RecipientType.PLAYER?snapshot.recipient().id():null,
                eligibility.write(snapshot),actor.getId(),java.sql.Timestamp.from(snapshot.issuedAt()),request.requestId(),eligibility.write(request),request.reissuedFrom());
        eligibility.audit(cid,snapshot.id(),snapshot.approvalId(),actor.getId(),request.reissuedFrom()==null?"ISSUED":"REISSUED",request.reason()==null?"Approved competition result":request.reason().trim());
        return new Issued(snapshot,"ISSUED",null,null);
    }
    @Transactional
    public Snapshot preview(UUID cid,User actor,IssueRequest request) {
        eligibility.lock(cid);return prepare(cid,actor,request,true);
    }
    private Snapshot prepare(UUID cid,User actor,IssueRequest r,boolean preview) {
        var a=eligibility.require(cid,r.approvalId());
        Recipient recipient=eligibility.eligible(a,r.category(),r.recipientType()).stream().filter(p->p.id().equals(r.recipientId())).findFirst()
                .orElseThrow(()->conflict("Recipient is not eligible for this award in this championship"));
        recipient=eligibility.currentRecipient(cid,recipient);
        if(!preview && db.queryForObject("select count(*) from play_neml.certificates where championship_id=? and category=? and recipient_type=? and recipient_id=? and status='ISSUED'",Long.class,cid,r.category().name(),r.recipientType().name(),r.recipientId())>0)
            throw conflict("An active certificate already exists; revoke it before issuing a correction");
        if(r.reissuedFrom()!=null) {
            var old=get(cid,r.reissuedFrom());
            if(!old.status().equals("REVOKED") || old.certificate().category()!=r.category() || old.certificate().recipient().type()!=r.recipientType())
                throw conflict("Reissue must replace a revoked certificate of the same category and recipient type");
            if(r.reason()==null||r.reason().trim().length()<10)throw conflict("A correction justification of at least 10 characters is required");
            if(db.queryForObject("select count(*) from play_neml.certificates where reissued_from=?",Long.class,r.reissuedFrom())>0)
                throw conflict("This certificate already has a replacement");
        }
        UUID id=UUID.randomUUID();String ref="PN-"+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT);
        String achievement=switch(r.category()) {case WINNER->"Awarded for becoming championship champions.";case RUNNER_UP->"Awarded for achieving the runner-up position.";case PARTICIPATION->"In recognition of verified participation and sporting commitment.";};
        var branding=db.queryForList("select display_name,primary_color from play_neml.championship_branding_configurations where championship_id=? order by configuration_version desc limit 1",cid);
        String title=branding.isEmpty()?a.championship():branding.get(0).get("display_name").toString();
        String accent=r.category()==Category.WINNER?"#AF832E":r.category()==Category.RUNNER_UP?"#67778B":"#16776C";
        if(r.category()==Category.PARTICIPATION&&!branding.isEmpty()&&branding.get(0).get("primary_color").toString().matches("#[0-9A-Fa-f]{6}")) {
            var color=java.awt.Color.decode(branding.get(0).get("primary_color").toString());
            // Bright screen branding is darkened for legibility on ivory paper.
            while(.2126*color.getRed()+.7152*color.getGreen()+.0722*color.getBlue()>145)color=color.darker();
            accent=String.format("#%02X%02X%02X",color.getRed(),color.getGreen(),color.getBlue());
        }
        return new Snapshot(1,id,ref,cid,title,a.sport(),"Championship · Finalized competition",a.id(),r.category(),recipient,achievement,
                Instant.now(),actor.getFullName(),"Championship Administrator",accent,r.reissuedFrom(),verification.url(ref));
    }
    @Transactional(readOnly=true)
    public Issued get(UUID cid,UUID id) {
        return db.query("select snapshot,status,revoked_at,revocation_reason from play_neml.certificates where championship_id=? and id=?",
                (rs,n)->new Issued(eligibility.read(rs.getString(1),Snapshot.class),rs.getString(2),rs.getTimestamp(3)==null?null:rs.getTimestamp(3).toInstant(),rs.getString(4)),cid,id)
                .stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Certificate not found"));
    }
    @Transactional(readOnly=true)
    public History history(UUID cid,String search,String category,String status,int page) {
        if(page<0 || page>100000 || search.length()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid history filter");
        if(!category.isEmpty())try{Category.valueOf(category);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid category");}
        if(!status.isEmpty()&&!Set.of("ISSUED","REVOKED").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid status");
        String where=" championship_id=? and (?='' or category=?) and (?='' or status=?) and (lower(snapshot::json->'recipient'->>'name') like ? or lower(public_reference) like ?)";
        String term="%"+search.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        var items=db.query("select snapshot,status,revoked_at,revocation_reason from play_neml.certificates where"+where+" order by issued_at desc,id limit 25 offset ?",
                (rs,n)->new Issued(eligibility.read(rs.getString(1),Snapshot.class),rs.getString(2),rs.getTimestamp(3)==null?null:rs.getTimestamp(3).toInstant(),rs.getString(4)),cid,category,category,status,status,term,term,page*25);
        Long total=db.queryForObject("select count(*) from play_neml.certificates where"+where,Long.class,cid,category,category,status,status,term,term);
        Map<String,Long> counts=new HashMap<>();
        db.query("select category,status,count(*) from play_neml.certificates where championship_id=? group by category,status",rs->{
            counts.merge(rs.getString(1),rs.getLong(3),Long::sum);counts.merge(rs.getString(2),rs.getLong(3),Long::sum);counts.merge("TOTAL",rs.getLong(3),Long::sum);
        },cid);
        return new History(items,total,counts);
    }
    @Transactional
    public Issued revoke(UUID cid,UUID id,UUID actor,String reason) {
        if(reason==null||reason.trim().length()<10||reason.length()>1000)throw conflict("Provide a revocation reason of 10 to 1000 characters");
        eligibility.lock(cid);var old=get(cid,id);
        if(old.status().equals("REVOKED"))return old;
        db.update("update play_neml.certificates set status='REVOKED',revoked_at=now(),revoked_by=?,revocation_reason=? where id=? and championship_id=?",actor,reason.trim(),id,cid);
        eligibility.audit(cid,id,old.certificate().approvalId(),actor,"REVOKED",reason.trim());return get(cid,id);
    }
}
