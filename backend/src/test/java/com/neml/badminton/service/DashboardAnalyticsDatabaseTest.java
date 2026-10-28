package com.neml.badminton.service;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.security.ViewerPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.math.BigDecimal;
import java.util.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="PHASE6_DATABASE_TESTS",matches="true")
class DashboardAnalyticsDatabaseTest {
 @Autowired DashboardAnalyticsService service;@Autowired ChampionshipRepository championships;@Autowired TeamRepository teams;
 @Autowired PlayerRepository players;@Autowired UserRepository users;@Autowired ChampionshipRoleRepository roles;
 @Autowired MatchRepository matches;@Autowired JdbcTemplate db;@Autowired MockMvc mvc;
 Championship c;User admin,captain,platform;Team a,b;
 @BeforeEach void fixture(){
  c=championships.save(Championship.builder().name("Analytics fixture").roomCode(UUID.randomUUID().toString().substring(0,20)).sportType("BADMINTON").build());
  admin=user("Administrator",Role.USER);captain=user("Captain",Role.USER);platform=user("Platform",Role.SUPER_ADMIN);
  a=team("Scoped Aces","ACE");b=team("Foreign Blazers","BLZ");
  roles.save(ChampionshipRole.builder().championship(c).user(admin).role(ChampionshipRoleType.CHAMPIONSHIP_ADMIN).build());
  roles.save(ChampionshipRole.builder().championship(c).user(captain).role(ChampionshipRoleType.TEAM_CAPTAIN).team(a).build());
  players.save(Player.builder().championship(c).fullName("Purchased player").gender(Gender.FEMALE).basePrice(BigDecimal.TEN).soldPrice(BigDecimal.TEN).status(PlayerStatus.SOLD).team(a).build());
  players.save(Player.builder().championship(c).fullName("Available player").gender(Gender.MALE).basePrice(BigDecimal.TEN).status(PlayerStatus.AVAILABLE).build());
  matches.save(com.neml.badminton.entity.Match.builder().championship(c).teamA(a).teamB(b).winnerTeam(a).matchNumber(1).fixtureKey("analytics-match").scheduledAt(Instant.now()).status(MatchStatus.COMPLETED).teamAFormatWins(1).teamBFormatWins(0).build());
 }
 User user(String name,Role role){return users.save(User.builder().fullName(name).email(UUID.randomUUID()+"@example.invalid").passwordHash("disabled-fixture").role(role).build());}
 Team team(String name,String code){return teams.save(Team.builder().championship(c).name(name).shortCode(code).purseTotal(BigDecimal.valueOf(100)).purseRemaining(BigDecimal.valueOf(90)).maleCount(0).femaleCount(1).build());}
 UsernamePasswordAuthenticationToken auth(Object user){return new UsernamePasswordAuthenticationToken(user,null,List.of());}
 @Test void aggregatesMatchRealDataAndCaptainOnlyReceivesOwnTeam(){
  var summary=service.summary(c.getId(),auth(admin));assertThat(summary.players()).containsEntry("SOLD",1L).containsEntry("AVAILABLE",1L);assertThat(summary.matches()).containsEntry("COMPLETED",1L);assertThat(summary.purses()).hasSize(2);assertThat(summary.certificates()).isEmpty();
  var own=service.summary(c.getId(),auth(captain));assertThat(own.players()).isNull();assertThat(own.certificates()).isNull();assertThat(own.purses()).isNull();assertThat(own.captain().team().id()).isEqualTo(a.getId());assertThat(own.captain().composition()).containsExactlyEntriesOf(Map.of("FEMALE",1L));assertThat(own.captain().played()).isEqualTo(1);assertThat(own.captain().won()).isEqualTo(1);assertThat(own.captain().recent()).hasSize(1);
  var global=service.platform();assertThat(global.participants()).isEqualTo(db.queryForObject("select count(*) from play_neml.players",Long.class));assertThat(global.championships()).isEqualTo(db.queryForObject("select count(*) from play_neml.championships",Long.class));
  assertThat(players.findAllByChampionshipIdAndTeamIdOrderByAuctionOrderAsc(c.getId(),a.getId())).hasSize(1);assertThat(players.findAllByChampionshipIdAndTeamIsNotNull(c.getId())).hasSize(1);
 }
 @Test void actualHttpSecurityAndResponsePrivacy()throws Exception {
  String path="/api/championships/"+c.getId()+"/analytics/summary";
  mvc.perform(get(path)).andExpect(status().isUnauthorized());
  mvc.perform(get(path).with(authentication(auth(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.players.SOLD").value(1)).andExpect(jsonPath("$.purses.length()").value(2));
  mvc.perform(get(path).with(authentication(auth(new ViewerPrincipal(c.getId()))))).andExpect(status().isOk()).andExpect(jsonPath("$.players").doesNotExist()).andExpect(jsonPath("$.certificates").doesNotExist()).andExpect(jsonPath("$.purses").doesNotExist()).andExpect(jsonPath("$.captain").doesNotExist());
  mvc.perform(get(path.replace(c.getId().toString(),UUID.randomUUID().toString())).with(authentication(auth(admin)))).andExpect(status().isForbidden());
  mvc.perform(get("/api/platform/analytics").with(authentication(auth(admin)))).andExpect(status().isForbidden());
  var superAuth=new UsernamePasswordAuthenticationToken(platform,null,List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
  mvc.perform(get("/api/platform/analytics").with(authentication(superAuth))).andExpect(status().isOk()).andExpect(jsonPath("$.participants").isNumber());
 }
}
