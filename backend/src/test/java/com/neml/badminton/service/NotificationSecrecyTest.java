package com.neml.badminton.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neml.badminton.entity.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class NotificationSecrecyTest {
    @Test void accountNoticeContainsNoPasswordOrSetupSecret(){
        var jdbc=mock(JdbcTemplate.class);
        var gateway=new MessageTrackerGateway(jdbc,new ObjectMapper());
        var user=User.builder().id(UUID.randomUUID()).fullName("Test").email("test@example.invalid").build();
        var championship=Championship.builder().id(UUID.randomUUID()).name("Test").roomCode("TEST").build();
        gateway.queueChampionshipAdmin(user,"9999999999",championship,"TEST");
        var args=ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(anyString(),args.capture(),args.capture(),args.capture(),args.capture(),args.capture(),
                args.capture(),args.capture(),args.capture(),args.capture(),args.capture(),args.capture(),args.capture());
        assertThat(args.getAllValues().toString()).doesNotContain("temporaryPassword","passwordHash","setupPath","token=","tokenHash");
        assertThat(args.getAllValues().toString()).contains("activationRequired");
    }
}
