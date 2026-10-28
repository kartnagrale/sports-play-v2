package com.neml.badminton.service;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named="PHASE4_DATABASE_TESTS",matches="true")
class PlayerImportDatabaseTest {
 @Autowired ChampionshipRepository championships;@Autowired AuctionRepository auctions;@Autowired TournamentSettingsRepository settings;
 @Autowired UserRepository users;@Autowired PlayerImportService imports;@Autowired PlayerRepository players;@Autowired JdbcTemplate jdbc;
 @Autowired PlayerPhotoService photos;@Autowired PlayerRegistrationService registration;
 @org.springframework.beans.factory.annotation.Value("${app.players.photo.directory}") String photoDirectory;
 UUID cid,actor;
 @BeforeEach void fixture(){
  var c=championships.save(Championship.builder().name("Import DB fixture").roomCode(UUID.randomUUID().toString().substring(0,20)).sportType("BADMINTON").lifecyclePhase(ChampionshipPhase.PLAYER_REGISTRATION).build());cid=c.getId();
  auctions.save(Auction.builder().championship(c).name("Fixture auction").build());settings.save(TournamentSettings.builder().championship(c).playerBasePrice(new BigDecimal("50000")).build());
  actor=users.save(User.builder().fullName("Fixture administrator").email(UUID.randomUUID()+"@example.invalid").role(Role.USER).passwordHash("disabled-fixture").build()).getId();
 }
 private PlayerImportService.Preview preview(String[][] rows)throws Exception{return imports.preview(cid,actor,PlayerWorkbookServiceTest.file(PlayerWorkbookServiceTest.workbook(rows)));}
 @Test void actualTransactionRollsBackEarlierInsertsWhenDatabaseRejectsLaterRow()throws Exception{
  var p=preview(new String[][]{{"Accept first","MALE","","","","OK"},{"REJECT-PHASE4","FEMALE","","","","FAIL"}});
  jdbc.execute("create function play_neml.reject_phase4_test() returns trigger language plpgsql as $$ begin if NEW.full_name='REJECT-PHASE4' then raise exception 'synthetic rollback test'; end if; return NEW; end $$");
  jdbc.execute("create trigger reject_phase4_test before insert on play_neml.players for each row execute function play_neml.reject_phase4_test()");
  try{assertThatThrownBy(()->imports.confirm(cid,actor,p.importId(),p.checksum(),false)).isInstanceOf(RuntimeException.class);assertThat(players.findAllByChampionshipIdOrderByAuctionOrderAsc(cid)).isEmpty();assertThat(imports.status(cid,actor,p.importId()).status()).isEqualTo("PREVIEW");}
  finally{jdbc.execute("drop trigger reject_phase4_test on play_neml.players");jdbc.execute("drop function play_neml.reject_phase4_test()");}
  assertThat(imports.confirm(cid,actor,p.importId(),p.checksum(),false).imported()).isEqualTo(2);
  assertThat(jdbc.queryForObject("select payload from play_neml.player_imports where id=?",String.class,p.importId())).isNull();
 }
 @Test void distinctConcurrentImportsWithSameReferenceHaveOneInsertAndOneSkip()throws Exception{
  var a=preview(new String[][]{{"One","MALE","","","","SAME-REF"}});var b=preview(new String[][]{{"Other person","MALE","","","","SAME-REF"}});
  var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
  try{var one=pool.submit(()->{gate.await();return imports.confirm(cid,actor,a.importId(),a.checksum(),false);});var two=pool.submit(()->{gate.await();return imports.confirm(cid,actor,b.importId(),b.checksum(),false);});gate.countDown();var x=one.get(15,TimeUnit.SECONDS);var y=two.get(15,TimeUnit.SECONDS);assertThat(x.imported()+y.imported()).isEqualTo(1);assertThat(x.skipped()+y.skipped()).isEqualTo(1);assertThat(players.findAllByChampionshipIdOrderByAuctionOrderAsc(cid)).hasSize(1);}finally{pool.shutdownNow();}
 }
 @Test void expiryActorAndChampionshipBindingAndLifecycleRevalidation()throws Exception{
  var p=preview(new String[][]{{"One","MALE","","","","REF"}});
  assertThatThrownBy(()->imports.confirm(UUID.randomUUID(),actor,p.importId(),p.checksum(),false)).hasMessageContaining("not found");
  assertThatThrownBy(()->imports.confirm(cid,UUID.randomUUID(),p.importId(),p.checksum(),false)).hasMessageContaining("not found");
  jdbc.update("update play_neml.championships set lifecycle_phase='LEAGUE_STAGE' where id=?",cid);
  assertThatThrownBy(()->imports.confirm(cid,actor,p.importId(),p.checksum(),false)).hasMessageContaining("closed");
  jdbc.update("update play_neml.championships set lifecycle_phase='PLAYER_REGISTRATION' where id=?",cid);
  jdbc.update("update play_neml.player_imports set expires_at=? where id=?",java.sql.Timestamp.from(Instant.now().minusSeconds(1)),p.importId());
  assertThat(imports.status(cid,actor,p.importId()).status()).isEqualTo("EXPIRED");assertThat(jdbc.queryForObject("select payload from play_neml.player_imports where id=?",String.class,p.importId())).isNull();
  assertThatThrownBy(()->imports.confirm(cid,actor,p.importId(),p.checksum(),false)).hasMessageContaining("expired");
 }
 @Test void rosterConfirmationBlocksImportEvenWhenPhaseIsRegistration()throws Exception{
  var p=preview(new String[][]{{"One","MALE","","","","REF"}});var team=UUID.randomUUID();
  jdbc.update("insert into play_neml.teams(id,championship_id,name,short_code,purse_total,purse_remaining,male_count,female_count,version) values(?,?,'Roster Team','TEST',100,100,0,0,0)",team,cid);
  jdbc.update("insert into play_neml.squad_confirmations(id,championship_id,team_id,status,version,updated_at,created_at) values(?,?,?,'LOCKED',0,now(),now())",UUID.randomUUID(),cid,team);
  assertThatThrownBy(()->imports.confirm(cid,actor,p.importId(),p.checksum(),false)).hasMessageContaining("blocked after confirmation");
  assertThat(players.findAllByChampionshipIdOrderByAuctionOrderAsc(cid)).isEmpty();
 }
 @Test void photoReplacementRemovalAndRollbackPreserveSoldRosterAfterLock()throws Exception{
  var player=registration.register(cid,List.of(new PlayerRegistrationService.Registration("Photo Player",Gender.FEMALE,null,null,null,"PHOTO-1"))).get(0);UUID pid=player.getId(),team=UUID.randomUUID();
  jdbc.update("insert into play_neml.teams(id,championship_id,name,short_code,purse_total,purse_remaining,male_count,female_count,version) values(?,?,'Photo Team','PHOTO',1000,900,0,1,0)",team,cid);
  jdbc.update("update play_neml.players set status='SOLD',sold_price=777.77,team_id=? where id=?",team,pid);
  jdbc.update("insert into play_neml.squad_confirmations(id,championship_id,team_id,status,version,updated_at,created_at) values(?,?,?,'LOCKED',0,now(),now())",UUID.randomUUID(),cid,team);
  jdbc.update("update play_neml.championships set lifecycle_phase='LEAGUE_STAGE' where id=?",cid);
  var source=PlayerImageProcessorTest.file(PlayerImageProcessorTest.image("png",160,120,java.awt.Color.BLUE));
  var one=photos.upload(cid,pid,actor,source);String first=one.photoUrl().split("version=")[1];assertThat(photos.read(cid,pid,first)).isNotEmpty();
  var before=storedFiles();assertThatThrownBy(()->photos.upload(cid,pid,UUID.randomUUID(),source)).isInstanceOf(RuntimeException.class);assertThat(storedFiles()).isEqualTo(before);assertThat(photos.read(cid,pid,first)).isNotEmpty();
  var two=photos.upload(cid,pid,actor,source);String second=two.photoUrl().split("version=")[1];assertThat(second).isNotEqualTo(first);assertThatThrownBy(()->photos.read(cid,pid,first)).hasMessageContaining("not found");
  assertThat(two.status()).isEqualTo(PlayerStatus.SOLD);assertThat(two.soldPrice()).isEqualByComparingTo("777.77");assertThat(two.teamId()).isEqualTo(team);assertThat(two.playerReference()).isEqualTo("PHOTO-1");
  assertThatThrownBy(()->photos.read(UUID.randomUUID(),pid,second)).hasMessageContaining("not found");
  var removed=photos.remove(cid,pid,actor);assertThat(removed.photoUrl()).isNull();assertThat(removed.status()).isEqualTo(PlayerStatus.SOLD);assertThat(removed.teamId()).isEqualTo(team);
  assertThat(jdbc.queryForObject("select status from play_neml.squad_confirmations where team_id=?",String.class,team)).isEqualTo("LOCKED");assertThatThrownBy(()->photos.read(cid,pid,second)).hasMessageContaining("not found");
 }
 private List<String> storedFiles()throws Exception{var root=java.nio.file.Path.of(photoDirectory);if(!java.nio.file.Files.exists(root))return List.of();try(var stream=java.nio.file.Files.list(root)){return stream.map(p->p.getFileName().toString()).sorted().toList();}}
}
