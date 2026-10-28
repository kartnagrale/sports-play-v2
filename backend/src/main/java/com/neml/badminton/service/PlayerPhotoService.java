package com.neml.badminton.service;
import com.neml.badminton.dto.Dtos.PlayerDto;
import com.neml.badminton.entity.Player;
import com.neml.badminton.repository.*;
import com.neml.badminton.websocket.AuctionBroadcaster;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.slf4j.*;
import java.util.*;
import java.time.Instant;
import java.io.IOException;
@Service
public class PlayerPhotoService {
 private static final Logger log=LoggerFactory.getLogger(PlayerPhotoService.class);
 private final PlayerRepository players;private final ChampionshipRepository championships;private final PlayerPhotoStorage storage;private final PlayerImageProcessor images;private final AuctionBroadcaster broadcaster;
 public PlayerPhotoService(PlayerRepository players,ChampionshipRepository championships,PlayerPhotoStorage storage,PlayerImageProcessor images,AuctionBroadcaster broadcaster){this.players=players;this.championships=championships;this.storage=storage;this.images=images;this.broadcaster=broadcaster;}
 @Transactional public PlayerDto upload(UUID cid,UUID pid,UUID actor,MultipartFile file){
  byte[] jpeg=images.normalize(file);Player p=locked(cid,pid);UUID old=p.getPhotoKey(),key=UUID.randomUUID();
  // Register compensation before writing. The DB never commits a reference to a failed write.
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
   @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)cleanup(key);}
   @Override public void afterCommit(){cleanup(old);}
  });
  try{storage.put(key,jpeg);}catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Photo storage is unavailable. Please retry.");}
  p.setPhotoKey(key);p.setPhotoSize(jpeg.length);p.setPhotoUpdatedAt(Instant.now());p.setPhotoUpdatedBy(actor);players.saveAndFlush(p);notifyChange(p);
  log.info("Player photograph saved championship={} player={} actor={}",cid,pid,actor);return PlayerDto.from(p);
 }
 @Transactional public PlayerDto remove(UUID cid,UUID pid,UUID actor){
  Player p=locked(cid,pid);UUID old=p.getPhotoKey();p.setPhotoKey(null);p.setPhotoSize(null);p.setPhotoUpdatedAt(Instant.now());p.setPhotoUpdatedBy(actor);players.saveAndFlush(p);
  if(old!=null)TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){cleanup(old);}});
  notifyChange(p);log.info("Player photograph removed championship={} player={} actor={}",cid,pid,actor);return PlayerDto.from(p);
 }
 @Transactional(readOnly=true) public byte[] read(UUID cid,UUID pid,String version){
  Player p=players.findByIdAndChampionshipId(pid,cid).orElseThrow(()->notFound());
  if(p.getPhotoKey()==null||!p.getPhotoKey().toString().equals(version))throw notFound();
  try{return storage.read(p.getPhotoKey());}catch(IOException e){throw notFound();}
 }
 private Player locked(UUID cid,UUID pid){championships.findByIdForUpdate(cid).orElseThrow(()->notFound());return players.findByIdAndChampionshipId(pid,cid).orElseThrow(()->notFound());}
 private void notifyChange(Player p){if(p.getAuction()!=null)broadcaster.broadcastEvent(p.getChampionship().getId(),p.getAuction().getId(),"PLAYER_PHOTO_UPDATED",Map.of("playerId",p.getId()));}
 private void cleanup(UUID key){if(key!=null)try{storage.delete(key);}catch(IOException e){log.warn("Player photograph cleanup requires retry key={}",key);}}
 private static ResponseStatusException notFound(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Player photograph not found");}
}
