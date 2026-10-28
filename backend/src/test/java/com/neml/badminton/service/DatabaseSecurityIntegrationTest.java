package com.neml.badminton.service;

import com.neml.badminton.entity.User;
import com.neml.badminton.entity.Role;
import com.neml.badminton.repository.*;
import com.neml.badminton.seed.ReferenceDataInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="PHASE1_DB_URL",matches="jdbc:postgresql://(?:127\\.0\\.0\\.1|localhost):55441/phase1(?:\\?.*)?")
@DataJpaTest(properties={"spring.datasource.url=${PHASE1_DB_URL}","spring.datasource.username=phase1",
        "spring.datasource.password=${PHASE1_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=validate","spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({AccountActivationService.class,ReferenceDataInitializer.class,DatabaseSecurityIntegrationTest.Encoder.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class DatabaseSecurityIntegrationTest {
    @TestConfiguration static class Encoder {@Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(4);}}
    @Autowired UserRepository users;
    @Autowired AccountActivationService activations;
    @Autowired JdbcTemplate jdbc;
    @Autowired ReferenceDataInitializer referenceData;

    @Test void concurrentActivationHasExactlyOneWinnerAndStoresOnlyHash() throws Exception {
        var user=users.save(User.builder().email(UUID.randomUUID()+"@example.invalid").fullName("Concurrency test")
                .passwordHash("disabled").accountEnabled(false).role(Role.USER).build());
        var invite=activations.issue(user);
        String token=invite.setupPath().split("=")[1];
        assertThat(jdbc.queryForObject("select token_hash from play_neml.account_activations where user_id=?",String.class,user.getId()))
                .isEqualTo(AccountActivationService.hash(token)).doesNotContain(token);
        var executor=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {
            Callable<Boolean> attempt=()->{ready.countDown();start.await();try{activations.activate(token,"Concurrency-password-42");return true;}catch(org.springframework.web.server.ResponseStatusException rejected){return false;}};
            var one=executor.submit(attempt);var two=executor.submit(attempt);
            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();start.countDown();
            assertThat(List.of(one.get(15,TimeUnit.SECONDS),two.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(users.findById(user.getId()).orElseThrow().getAccountEnabled()).isTrue();
        }finally{start.countDown();executor.shutdownNow();}
    }
    @Test void reissuingSetupInvalidatesOlderUnusedLink(){
        var user=users.save(User.builder().email(UUID.randomUUID()+"@example.invalid").fullName("Renewal test")
                .passwordHash("disabled").accountEnabled(false).role(Role.USER).build());
        var old=activations.issue(user);var replacement=activations.issue(user);
        assertThatThrownBy(()->activations.activate(old.setupPath().split("=")[1],"Old-link-password-42"))
                .hasMessageContaining("invalid or expired");
        activations.activate(replacement.setupPath().split("=")[1],"New-link-password-42");
    }
    @Test @Transactional void referenceDataIsIndependentIdempotentAndPreservesCustomization(){
        referenceData.run();
        Integer count=jdbc.queryForObject("select count(*) from play_neml.app_screens",Integer.class);
        jdbc.update("update play_neml.app_screens set label='Custom Dashboard' where code='DASHBOARD'");
        jdbc.update("update play_neml.role_screen_mappings set visible=false where role_key='SPECTATOR'");
        referenceData.run();
        assertThat(jdbc.queryForObject("select count(*) from play_neml.app_screens",Integer.class)).isEqualTo(count);
        assertThat(jdbc.queryForObject("select label from play_neml.app_screens where code='DASHBOARD'",String.class)).isEqualTo("Custom Dashboard");
        assertThat(jdbc.queryForObject("select count(*) from play_neml.role_screen_mappings where role_key='SPECTATOR' and visible",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from play_neml.app_screens where code='COMPETITION'",Integer.class)).isEqualTo(1);
    }
}
