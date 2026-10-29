package com.neml.badminton.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neml.badminton.repository.*;
import com.neml.badminton.service.PlayerWorkbookService.Row;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.security.MessageDigest;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;

@Service
public class PlayerImportService {
    public record Preview(UUID importId,UUID championshipId,String checksum,String status,Instant expiresAt,
                          int imported,int skipped,int invalid,int unreferenced,List<Row> rows) {}
    private final PlayerWorkbookService workbooks;private final PlayerRegistrationService registration;
    private final PlayerRepository players;private final ChampionshipRepository championships;
    private final JdbcTemplate jdbc;private final ObjectMapper mapper;private final Duration lifetime;
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay=60000)
    public void expirePreviews(){jdbc.update("update play_neml.player_imports set payload=null,status='EXPIRED' where expires_at < now() and payload is not null and status <> 'COMPLETED'");}
    public PlayerImportService(PlayerWorkbookService workbooks,PlayerRegistrationService registration,PlayerRepository players,
            ChampionshipRepository championships,JdbcTemplate jdbc,ObjectMapper mapper,@Value("${app.players.import.preview-lifetime:PT15M}") Duration lifetime){
        this.workbooks=workbooks;this.registration=registration;this.players=players;this.championships=championships;this.jdbc=jdbc;this.mapper=mapper;this.lifetime=lifetime;
    }
    @Transactional
    public Preview preview(UUID cid,UUID actor,MultipartFile file){
        List<Row> rows=workbooks.parse(file);String hash;
        try{hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file.getBytes()));}catch(Exception e){throw new IllegalArgumentException("Cannot read workbook");}
        registration.lockMutable(cid);
        // Expired previews retain only metadata/counts. Raw workbook bytes are never retained.
        jdbc.update("update play_neml.player_imports set payload=null,status='EXPIRED' where expires_at < now() and payload is not null and status <> 'COMPLETED'");
        var previous=jdbc.queryForList("select id,status,actor_id from play_neml.player_imports where championship_id=? and checksum=?",cid,hash);
        if(!previous.isEmpty()){
            var old=previous.get(0);
            if(!actor.equals(old.get("actor_id")))throw new ResponseStatusException(HttpStatus.CONFLICT,"This workbook already has an import owned by another administrator");
            if("COMPLETED".equals(old.get("status")))return status(cid,actor,(UUID)old.get("id"));
        }
        rows=validateState(cid,rows);UUID id=previous.isEmpty()?UUID.randomUUID():(UUID)previous.get(0).get("id");Instant expires=Instant.now().plus(lifetime);
        int invalid=(int)rows.stream().filter(r->!r.errors().isEmpty()).count();String state=invalid==0?"PREVIEW":"INVALID";
        String filename=Optional.ofNullable(file.getOriginalFilename()).orElse("workbook.xlsx").replaceAll("[^a-zA-Z0-9._-]","_");if(filename.length()>150)filename=filename.substring(filename.length()-150);
        try{jdbc.update("insert into play_neml.player_imports(id,championship_id,actor_id,checksum,file_name,file_size,expires_at,status,payload,invalid_count) values(?,?,?,?,?,?,?,?,?,?) on conflict (id) do update set expires_at=excluded.expires_at,status=excluded.status,payload=excluded.payload,invalid_count=excluded.invalid_count",id,cid,actor,hash,filename,file.getSize(),Timestamp.from(expires),state,mapper.writeValueAsString(rows),invalid);}
        catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Cannot prepare import");}
        return response(id,cid,hash,state,expires,0,rows);
    }
    @Transactional
    public Preview status(UUID cid,UUID actor,UUID id){
        var list=jdbc.queryForList("select * from play_neml.player_imports where id=? and championship_id=? and actor_id=?",id,cid,actor);
        if(list.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Import not found");
        var data=list.get(0);String state=(String)data.get("status");Instant expires=((Timestamp)data.get("expires_at")).toInstant();
        if(!state.equals("COMPLETED")&&expires.isBefore(Instant.now())){jdbc.update("update play_neml.player_imports set payload=null,status='EXPIRED' where id=?",id);state="EXPIRED";data.put("payload",null);}
        List<Row> rows=readRows(data.get("payload"));
        if(state.equals("COMPLETED")||state.equals("EXPIRED"))return new Preview(id,cid,(String)data.get("checksum"),state,expires,((Number)data.get("imported_count")).intValue(),((Number)data.get("skipped_count")).intValue(),((Number)data.get("invalid_count")).intValue(),0,List.of());
        return response(id,cid,(String)data.get("checksum"),state,expires,0,rows);
    }
    @Transactional
    public Preview confirm(UUID cid,UUID actor,UUID id,String checksum,boolean allowUnreferenced){
        // Shared lock order with manual create/update and squad confirmation: championship before all other rows.
        championships.findByIdForUpdate(cid).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Championship not found"));
        Preview preview=status(cid,actor,id);
        if(!Objects.equals(checksum,preview.checksum()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Preview checksum does not match");
        if(preview.status().equals("COMPLETED"))return preview;
        if(!preview.status().equals("PREVIEW"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Import is invalid or expired. Upload a corrected workbook.");
        registration.lockMutable(cid);
        List<Row> rows=validateState(cid,preview.rows());
        if(rows.stream().anyMatch(r->!r.errors().isEmpty()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Player data changed. Upload again and review row errors.");
        if(rows.stream().anyMatch(r->r.player().playerReference()==null)&&!allowUnreferenced)throw new ResponseStatusException(HttpStatus.CONFLICT,"Acknowledge the duplicate risk for rows without Player Reference");
        var additions=rows.stream().filter(r->!r.duplicate()).map(Row::player).toList();
        int imported=additions.isEmpty()?0:registration.register(cid,additions).size();int skipped=rows.size()-imported;
        jdbc.update("update play_neml.player_imports set status='COMPLETED',imported_count=?,skipped_count=?,payload=null,confirmed_at=now() where id=?",imported,skipped,id);
        return new Preview(id,cid,checksum,"COMPLETED",preview.expiresAt(),imported,skipped,0,0,List.of());
    }
    private List<Row> validateState(UUID cid,List<Row> rows){
        var existing=players.findAllByChampionshipIdOrderByAuctionOrderAsc(cid);Set<String> registered=new HashSet<>(),seen=new HashSet<>();Set<Integer> orders=new HashSet<>();
        existing.forEach(p->{if(p.getPlayerReference()!=null)registered.add(p.getPlayerReference());if(p.getAuctionOrder()!=null)orders.add(p.getAuctionOrder());});
        Map<String,Long> counts=new HashMap<>();rows.forEach(r->{if(r.player().playerReference()!=null)counts.merge(r.player().playerReference(),1L,Long::sum);});
        List<Row> result=new ArrayList<>();
        for(Row row:rows){var r=row.player();List<String> errors=new ArrayList<>(row.errors());boolean duplicate=r.playerReference()!=null&&registered.contains(r.playerReference());
            if(!duplicate&&existing.stream().anyMatch(p->p.getStatus()==com.neml.badminton.entity.PlayerStatus.PREASSIGNED&&p.getFullName().trim().equalsIgnoreCase(r.fullName().trim())))errors.add("Possible preassigned captain duplicate: use the existing captain Player Reference");
            if(r.playerReference()!=null&&counts.get(r.playerReference())>1)errors.add("Duplicate Player Reference in workbook");
            if(!duplicate&&r.auctionOrder()!=null&&!orders.add(r.auctionOrder()))errors.add("Auction Order is already assigned");
            result.add(new Row(row.rowNumber(),r,errors,duplicate));}
        return result;
    }
    private Preview response(UUID id,UUID cid,String hash,String state,Instant expires,int imported,List<Row> rows){return new Preview(id,cid,hash,state,expires,imported,(int)rows.stream().filter(Row::duplicate).count(),(int)rows.stream().filter(r->!r.errors().isEmpty()).count(),(int)rows.stream().filter(r->r.player().playerReference()==null).count(),rows);}
    private List<Row> readRows(Object value){if(value==null)return List.of();try{return mapper.readValue(value.toString(),new TypeReference<List<Row>>(){});}catch(Exception e){throw new IllegalStateException("Import could not be read");}}
}
