package com.neml.badminton.certificate;

import com.neml.badminton.certificate.CertificateModels.*;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.certificates.public-enabled=true","app.certificates.public-base-url=https://certificates.example.test"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="PHASE5_DATABASE_TESTS",matches="true")
class CertificateDatabaseTest {
    @Autowired CertificateEligibilityService eligibility;@Autowired CertificateService service;
    @Autowired ChampionshipRepository championships;@Autowired TeamRepository teams;@Autowired PlayerRepository players;
    @Autowired MatchRepository matches;@Autowired CompetitionConfigurationRepository configurations;
    @Autowired CompetitionStageRepository stages;@Autowired UserRepository users;@Autowired ChampionshipRoleRepository roles;
    @Autowired JdbcTemplate db;@Autowired MockMvc mvc;
    @Autowired CertificateBatchService batches;
    Championship c;User actor;Team a,b;com.neml.badminton.entity.Match fixture;UUID played,unused;
    @BeforeEach void setup() {
        c=championships.save(Championship.builder().name("Certificate Cup").sportType("BADMINTON").roomCode(UUID.randomUUID().toString().substring(0,20)).lifecyclePhase(ChampionshipPhase.COMPLETED).build());
        actor=users.save(User.builder().fullName("Award Administrator").email(UUID.randomUUID()+"@example.invalid").passwordHash("disabled-fixture").role(Role.USER).build());
        roles.save(ChampionshipRole.builder().championship(c).user(actor).role(ChampionshipRoleType.CHAMPIONSHIP_ADMIN).build());
        a=team("Aces","ACE");b=team("Blazers","BLZ");
        configurations.save(CompetitionConfiguration.builder().championship(c).sportCode("BADMINTON").champion(a).runnerUp(b).build());
        var stage=stages.save(CompetitionStage.builder().championship(c).stageType(CompetitionStageType.FINAL).stageOrder(2).status(CompetitionStageStatus.COMPLETED).build());
        fixture=matches.save(com.neml.badminton.entity.Match.builder().championship(c).stageChampionshipId(c.getId()).competitionStage(stage).stageType(CompetitionStageType.FINAL).teamA(a).teamB(b).winnerTeam(a).teamAFormatWins(1).teamBFormatWins(0).matchNumber(1).fixtureKey("FINAL").scheduledAt(Instant.now()).status(MatchStatus.COMPLETED).resultRevision(5).build());
        played=player("Élodie Fernández",a).getId();unused=player("Registered Only",a).getId();
        UUID format=UUID.randomUUID();db.update("insert into play_neml.match_formats(id,championship_id,match_id,format_type,format_order,score_a,score_b,completed,version) values(?,?,?,'MENS_SINGLES',1,21,9,true,0)",format,c.getId(),fixture.getId());
        db.update("insert into play_neml.format_side_a_players(format_id,player_id) values(?,?)",format,played);
    }
    Team team(String name,String code){return teams.save(Team.builder().championship(c).name(name).shortCode(code).purseTotal(BigDecimal.TEN).purseRemaining(BigDecimal.TEN).maleCount(0).femaleCount(0).build());}
    Player player(String name,Team team){return players.save(Player.builder().championship(c).fullName(name).gender(Gender.FEMALE).basePrice(BigDecimal.ONE).status(PlayerStatus.SOLD).team(team).build());}
    Approval approve(){return eligibility.approve(c.getId(),actor.getId(),"Final results reviewed and approved");}
    IssueRequest req(Approval p,Category cat,RecipientType type,UUID recipient){return new IssueRequest(UUID.randomUUID(),p.id(),cat,type,recipient,null,null);}
    @Test void authoritativeAwardsAndPlayedParticipationExcludeRegistrationOnly() {
        var approval=approve();
        assertThat(eligibility.eligible(approval,Category.WINNER,RecipientType.TEAM)).extracting(Recipient::id).containsExactly(a.getId());
        assertThat(eligibility.eligible(approval,Category.RUNNER_UP,RecipientType.TEAM)).extracting(Recipient::id).containsExactly(b.getId());
        assertThat(eligibility.eligible(approval,Category.PARTICIPATION,RecipientType.PLAYER)).extracting(Recipient::id).containsExactly(played);
        assertThat(eligibility.eligible(approval,Category.WINNER,RecipientType.PLAYER)).isEmpty();
        assertThatThrownBy(()->service.issue(c.getId(),actor,req(approval,Category.WINNER,RecipientType.TEAM,b.getId()))).hasMessageContaining("not eligible");
        assertThatThrownBy(()->service.issue(c.getId(),actor,req(approval,Category.PARTICIPATION,RecipientType.PLAYER,unused))).hasMessageContaining("not eligible");
        assertThatThrownBy(()->service.issue(c.getId(),actor,req(approval,Category.PARTICIPATION,RecipientType.PLAYER,UUID.randomUUID()))).hasMessageContaining("not eligible");
        var winner=service.issue(c.getId(),actor,req(approval,Category.WINNER,RecipientType.TEAM,a.getId()));
        var runner=service.issue(c.getId(),actor,req(approval,Category.RUNNER_UP,RecipientType.TEAM,b.getId()));
        assertThat(winner.certificate().reference()).isNotEqualTo(runner.certificate().reference());
        assertThat(service.history(c.getId(),"Aces","","",0).total()).isEqualTo(1);
    }
    @Test void incompleteAndInconsistentFinalRejectApproval() {
        db.update("update play_neml.matches set status='LIVE' where id=?",fixture.getId());
        assertThatThrownBy(this::approve).hasMessageContaining("Every competition fixture");
        db.update("update play_neml.matches set status='COMPLETED',winner_team_id=? where id=?",b.getId(),fixture.getId());
        assertThatThrownBy(this::approve).hasMessageContaining("disagree");
    }
    @Test void repeatedAndConcurrentRequestsAreOneIssuanceAndConflictsAreRejected()throws Exception {
        var p=approve();var r=req(p,Category.WINNER,RecipientType.TEAM,a.getId());
        var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
        try{var one=pool.submit(()->{gate.await();return service.issue(c.getId(),actor,r);});var two=pool.submit(()->{gate.await();return service.issue(c.getId(),actor,r);});gate.countDown();assertThat(one.get(30,TimeUnit.SECONDS).certificate().id()).isEqualTo(two.get(30,TimeUnit.SECONDS).certificate().id());}finally{pool.shutdownNow();}
        assertThat(service.history(c.getId(),"","","",0).total()).isEqualTo(1);
        assertThatThrownBy(()->service.issue(c.getId(),actor,req(p,Category.WINNER,RecipientType.TEAM,a.getId()))).hasMessageContaining("active certificate");
        assertThatThrownBy(()->service.issue(c.getId(),actor,new IssueRequest(r.requestId(),p.id(),Category.RUNNER_UP,RecipientType.TEAM,b.getId(),null,null))).hasMessageContaining("Idempotency");
        assertThat(db.queryForObject("select count(*) from play_neml.certificate_audit where championship_id=? and action='ISSUED'",Long.class,c.getId())).isEqualTo(1);
    }
    @Test void correctionsRevokeOriginalAndReissueHasNewReferenceAndImmutableHistory() {
        var p=approve();var request=req(p,Category.WINNER,RecipientType.TEAM,a.getId());var issued=service.issue(c.getId(),actor,request);
        db.update("update play_neml.teams set name='Renamed' where id=?",a.getId());assertThat(service.get(c.getId(),issued.certificate().id()).certificate().recipient().name()).isEqualTo("Aces");
        db.update("update play_neml.matches set result_revision=result_revision+1 where id=?",fixture.getId());
        assertThat(service.get(c.getId(),issued.certificate().id()).status()).isEqualTo("REVOKED");
        assertThat(service.issue(c.getId(),actor,request).status()).isEqualTo("REVOKED");
        assertThatThrownBy(()->service.issue(c.getId(),actor,req(p,Category.WINNER,RecipientType.TEAM,a.getId()))).hasMessageContaining("outdated");
        var next=approve();var corrected=service.issue(c.getId(),actor,new IssueRequest(UUID.randomUUID(),next.id(),Category.WINNER,RecipientType.TEAM,a.getId(),issued.certificate().id(),"Result revision corrected and approved"));
        assertThat(corrected.certificate().reference()).isNotEqualTo(issued.certificate().reference());
        assertThat(corrected.certificate().reissuedFrom()).isEqualTo(issued.certificate().id());
        assertThat(corrected.certificate().recipient().name()).isEqualTo("Renamed");
        assertThatThrownBy(()->db.update("update play_neml.certificates set snapshot='{}' where id=?",corrected.certificate().id())).hasMessageContaining("immutable");
        assertThat(service.revoke(c.getId(),corrected.certificate().id(),actor.getId(),"Incorrect recipient spelling detected").status()).isEqualTo("REVOKED");
        assertThat(service.revoke(c.getId(),corrected.certificate().id(),actor.getId(),"Already revoked certificate requested").status()).isEqualTo("REVOKED");
    }
    @Test void foreignApprovalsAndRecipientsRejectAndTransactionFailureRollsBackAudit() {
        var p=approve();var request=req(p,Category.WINNER,RecipientType.TEAM,a.getId());
        assertThatThrownBy(()->service.issue(UUID.randomUUID(),actor,request)).hasMessageContaining("not found");
        var absent=User.builder().id(UUID.randomUUID()).fullName("Missing Actor").build();
        assertThatThrownBy(()->service.issue(c.getId(),absent,request)).isInstanceOf(RuntimeException.class);
        assertThat(service.history(c.getId(),"","","",0).total()).isZero();
        assertThat(db.queryForObject("select count(*) from play_neml.certificate_audit where championship_id=? and action='ISSUED'",Long.class,c.getId())).isZero();
    }
    @Test void realMethodAndHttpSecurityRejectAnonymousSuperAdminCaptainAndForeignTenant()throws Exception {
        String path="/api/championships/"+c.getId()+"/admin/certificates";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        var auth=new UsernamePasswordAuthenticationToken(actor,null,List.of(new SimpleGrantedAuthority("ROLE_USER")));
        mvc.perform(get(path).with(authentication(auth))).andExpect(status().isOk());
        mvc.perform(get(path.replace(c.getId().toString(),UUID.randomUUID().toString())).with(authentication(auth))).andExpect(status().isForbidden());
        actor.setRole(Role.SUPER_ADMIN);mvc.perform(get(path).with(authentication(auth))).andExpect(status().isForbidden());actor.setRole(Role.USER);
        db.update("update play_neml.championship_roles set role='TEAM_CAPTAIN' where user_id=?",actor.getId());
        mvc.perform(get(path).with(authentication(auth))).andExpect(status().isForbidden());
        mvc.perform(post(path).with(authentication(auth)).with(csrf()).contentType("application/json").content(eligibility.write(req(approve(),Category.WINNER,RecipientType.TEAM,a.getId())))).andExpect(status().isForbidden());
    }
    @Test void cricketParticipationUsesPlayingXiInPlayedCompletedMatch() {
        db.update("update play_neml.championships set sport_type='CRICKET' where id=?",c.getId());
        assertThat(eligibility.eligible(approve(),Category.PARTICIPATION,RecipientType.PLAYER)).isEmpty();
        // A fresh approval after adding played innings uses the cricket boundary, never badminton rubbers.
        db.update("update play_neml.certificate_approvals set invalidated_at=now() where championship_id=?",c.getId());
        db.update("insert into play_neml.cricket_playing_members(id,championship_id,match_id,team_id,player_id,captain,wicketkeeper,locked_at) values(?,?,?,?,?,true,true,now())",UUID.randomUUID(),c.getId(),fixture.getId(),a.getId(),played);
        db.update("insert into play_neml.cricket_innings(id,championship_id,match_id,batting_team_id,innings_number,runs,wickets,legal_balls,status,started_at,version) values(?,?,?,?,1,10,0,6,'COMPLETED',now(),0)",UUID.randomUUID(),c.getId(),fixture.getId(),a.getId());
        assertThat(eligibility.eligible(approve(),Category.PARTICIPATION,RecipientType.PLAYER)).extracting(Recipient::id).containsExactly(played);
    }
    @Test void bulkRetriesDeduplicateRecipientsAndProduceValidZip()throws Exception {
        var p=approve();var request=new BulkRequest(UUID.randomUUID(),p.id(),Category.PARTICIPATION,RecipientType.TEAM,List.of(a.getId(),b.getId(),a.getId()));
        var batch=batches.issue(c.getId(),actor,request);assertThat(batch.certificates()).hasSize(2);
        assertThat(batches.issue(c.getId(),actor,request).id()).isEqualTo(batch.id());
        assertThat(batch.certificates().stream().map(i->i.certificate().reference()).distinct()).hasSize(2);
        try(var zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(batches.zip(batch)))) {
            int count=0;while(zip.getNextEntry()!=null){try(var pdf=org.apache.pdfbox.Loader.loadPDF(zip.readAllBytes())){assertThat(pdf.getNumberOfPages()).isEqualTo(1);}count++;}assertThat(count).isEqualTo(2);
        }
        assertThatThrownBy(()->batches.batch(UUID.randomUUID(),batch.id())).hasMessageContaining("not found");
        assertThatThrownBy(()->batches.issue(c.getId(),actor,new BulkRequest(request.requestId(),p.id(),Category.WINNER,RecipientType.TEAM,List.of(a.getId())))).hasMessageContaining("idempotency");
        assertThatThrownBy(()->batches.issue(c.getId(),actor,new BulkRequest(UUID.randomUUID(),p.id(),Category.PARTICIPATION,RecipientType.PLAYER,java.util.stream.IntStream.range(0,26).mapToObj(i->UUID.randomUUID()).toList()))).hasMessageContaining("1 to 25");
    }
    @Test void bulkWinnerAndDatabaseFailureRollBackTheWholeBatch() {
        var p=approve();var last=java.util.stream.Stream.of(a.getId(),b.getId()).sorted().toList().get(1);
        assertThatThrownBy(()->batches.issue(c.getId(),actor,new BulkRequest(UUID.randomUUID(),p.id(),Category.PARTICIPATION,RecipientType.PLAYER,List.of(played,unused)))).hasMessageContaining("not eligible");
        assertThat(service.history(c.getId(),"","","",0).total()).isZero();
        db.execute("create function play_neml.reject_certificate_test() returns trigger language plpgsql as $$ begin if NEW.recipient_id='"+last+"'::uuid then raise exception 'Test rejection after earlier issuance'; end if; return NEW; end $$");
        db.execute("create trigger reject_certificate_test before insert on play_neml.certificates for each row execute function play_neml.reject_certificate_test()");
        try {
            assertThatThrownBy(()->batches.issue(c.getId(),actor,new BulkRequest(UUID.randomUUID(),p.id(),Category.PARTICIPATION,RecipientType.TEAM,List.of(a.getId(),b.getId())))).isInstanceOf(RuntimeException.class);
            assertThat(service.history(c.getId(),"","","",0).total()).isZero();
            assertThat(db.queryForObject("select count(*) from play_neml.certificate_audit where championship_id=? and action='ISSUED'",Long.class,c.getId())).isZero();
            assertThat(db.queryForObject("select count(*) from play_neml.certificate_batches where championship_id=?",Long.class,c.getId())).isZero();
        }finally{db.execute("drop trigger reject_certificate_test on play_neml.certificates");db.execute("drop function play_neml.reject_certificate_test()");}
        assertThat(batches.issue(c.getId(),actor,new BulkRequest(UUID.randomUUID(),p.id(),Category.WINNER,RecipientType.TEAM,List.of(a.getId()))).certificates()).hasSize(1);
        db.update("update play_neml.matches set result_revision=result_revision+1 where id=?",fixture.getId());
        assertThatThrownBy(()->batches.issue(c.getId(),actor,new BulkRequest(UUID.randomUUID(),p.id(),Category.RUNNER_UP,RecipientType.TEAM,List.of(b.getId())))).hasMessageContaining("outdated");
    }
    @Test void concurrentBulkRetriesCommitOneBatch()throws Exception {
        var p=approve();var request=new BulkRequest(UUID.randomUUID(),p.id(),Category.PARTICIPATION,RecipientType.TEAM,List.of(a.getId(),b.getId()));
        var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
        try{var x=pool.submit(()->{gate.await();return batches.issue(c.getId(),actor,request);});var y=pool.submit(()->{gate.await();return batches.issue(c.getId(),actor,request);});gate.countDown();assertThat(x.get(30,TimeUnit.SECONDS).id()).isEqualTo(y.get(30,TimeUnit.SECONDS).id());}finally{pool.shutdownNow();}
        assertThat(service.history(c.getId(),"","","",0).total()).isEqualTo(2);
    }
    @Test void publicVerificationIsMinimalAndRevocationChangesItsStatusImmediately()throws Exception {
        var issued=service.issue(c.getId(),actor,req(approve(),Category.WINNER,RecipientType.TEAM,a.getId()));
        var url="/api/certificate-verification/"+issued.certificate().reference();
        assertThat(issued.certificate().verificationUrl()).isEqualTo("https://certificates.example.test/verify?reference="+issued.certificate().reference());
        String response=mvc.perform(get(url)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VALID")).andExpect(jsonPath("$.recipient").value("Aces")).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("email","playerId","recipientId","championshipId","issuer","approval","photoUrl","password","token");
        service.revoke(c.getId(),issued.certificate().id(),actor.getId(),"Certificate must be corrected for presentation");
        mvc.perform(get(url)).andExpect(jsonPath("$.status").value("REVOKED"));
        mvc.perform(get("/api/certificate-verification/not-a-certificate")).andExpect(status().isOk()).andExpect(content().json("{\"status\":\"INVALID\"}"));
        mvc.perform(get("/api/certificate-verification/PN-00000000000000000000000000000000")).andExpect(status().isOk()).andExpect(content().json("{\"status\":\"INVALID\"}"));
    }
    @Test void unauthorizedBulkAndOversizedJsonAreRejected()throws Exception {
        var p=approve();String path="/api/championships/"+c.getId()+"/admin/certificates/batches";
        var request=new BulkRequest(UUID.randomUUID(),p.id(),Category.WINNER,RecipientType.TEAM,List.of(a.getId()));
        var auth=new UsernamePasswordAuthenticationToken(actor,null,List.of(new SimpleGrantedAuthority("ROLE_USER")));
        mvc.perform(post(path).with(authentication(auth)).with(csrf()).contentType("application/json").content(" ".repeat(17000))).andExpect(status().isPayloadTooLarge());
        db.update("update play_neml.championship_roles set role='TEAM_CAPTAIN' where user_id=?",actor.getId());
        mvc.perform(post(path).with(authentication(auth)).with(csrf()).contentType("application/json").content(eligibility.write(request))).andExpect(status().isForbidden());
    }
}
