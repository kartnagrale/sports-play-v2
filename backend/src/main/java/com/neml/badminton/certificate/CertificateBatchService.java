package com.neml.badminton.certificate;

import com.neml.badminton.certificate.CertificateModels.*;
import com.neml.badminton.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static com.neml.badminton.certificate.CertificateEligibilityService.conflict;

/** Small atomic synchronous batches: <=25 recipients, <=8MiB ZIP, <=2 concurrent requests per process. */
@Service
public class CertificateBatchService {
    private final JdbcTemplate db;private final CertificateService service;private final CertificateEligibilityService json;private final CertificatePdfRenderer renderer;
    public CertificateBatchService(JdbcTemplate db,CertificateService service,CertificateEligibilityService json,CertificatePdfRenderer renderer){this.db=db;this.service=service;this.json=json;this.renderer=renderer;}
    @Transactional(timeout=90)
    public Batch issue(UUID cid,User actor,BulkRequest request) {
        if(request.recipientIds()==null||request.recipientIds().isEmpty()||request.recipientIds().size()>25||request.recipientIds().stream().anyMatch(Objects::isNull))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select 1 to 25 eligible recipients");
        var recipients=request.recipientIds().stream().distinct().sorted().toList();
        var normalized=new BulkRequest(request.requestId(),request.approvalId(),request.category(),request.recipientType(),recipients);
        json.lock(cid);
        var previous=db.queryForList("select id,issued_by,request_payload,certificate_ids from play_neml.certificate_batches where championship_id=? and request_id=?",cid,request.requestId());
        if(!previous.isEmpty()) {
            var row=previous.get(0);
            if(!actor.getId().equals(row.get("issued_by"))||!json.write(normalized).equals(row.get("request_payload")))throw conflict("Batch idempotency key conflicts with a different request");
            return batch(cid,(UUID)row.get("id"));
        }
        List<Issued> certificates=new ArrayList<>();
        for(UUID recipient:recipients) {
            UUID key=UUID.nameUUIDFromBytes((cid+":"+request.requestId()+":"+request.recipientType()+":"+recipient).getBytes(StandardCharsets.UTF_8));
            certificates.add(service.issue(cid,actor,new IssueRequest(key,request.approvalId(),request.category(),request.recipientType(),recipient,null,null)));
        }
        var id=UUID.randomUUID();
        db.update("insert into play_neml.certificate_batches(id,championship_id,request_id,issued_by,request_payload,certificate_ids) values(?,?,?,?,?,?)",id,cid,request.requestId(),actor.getId(),json.write(normalized),json.write(certificates.stream().map(i->i.certificate().id()).toList()));
        return new Batch(id,List.copyOf(certificates));
    }
    @Transactional(readOnly=true)
    public Batch batch(UUID cid,UUID id) {
        var rows=db.queryForList("select certificate_ids from play_neml.certificate_batches where championship_id=? and id=?",cid,id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Certificate batch not found");
        UUID[] ids=json.read(rows.get(0).get("certificate_ids").toString(),UUID[].class);
        return new Batch(id,Arrays.stream(ids).map(x->service.get(cid,x)).toList());
    }
    public byte[] zip(Batch batch) {
        if(batch.certificates().size()>25)throw new IllegalStateException("Invalid stored batch size");
        try(var bytes=new ByteArrayOutputStream();var zip=new ZipOutputStream(bytes,StandardCharsets.UTF_8)) {
            int work=0;
            for(var item:batch.certificates()) {
                byte[] pdf=renderer.render(item.certificate(),false);work+=pdf.length;
                if(work>8*1024*1024)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Archive limit exceeded; download certificates individually");
                var entry=new ZipEntry(item.certificate().reference()+".pdf");entry.setTime(item.certificate().issuedAt().toEpochMilli());zip.putNextEntry(entry);zip.write(pdf);zip.closeEntry();
            }
            zip.finish();return bytes.toByteArray();
        }catch(IOException e){throw new IllegalStateException("Unable to create certificate archive",e);}
    }
}
