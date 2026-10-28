package com.neml.badminton.service;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountActivationServiceTest {
    final AccountActivationRepository tokens=mock(AccountActivationRepository.class);
    final UserRepository users=mock(UserRepository.class);
    final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    final AccountActivationService service=new AccountActivationService(tokens,users,encoder,Duration.ofMinutes(30));
    @Test void issuePersistsOnlyHashAndReturnsFragmentOnce(){
        var user=User.builder().id(UUID.randomUUID()).accountEnabled(false).build();
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        var invitation=service.issue(user);
        String token=invitation.setupPath().split("=")[1];
        assertThat(token).hasSize(43);
        verify(tokens).save(argThat(t->t.getTokenHash().equals(AccountActivationService.hash(token))
                &&!t.getTokenHash().contains(token)&&t.getUser()==user&&t.getExpiresAt().equals(invitation.expiresAt())));
    }
    @Test void validActivationHashesPasswordEnablesUserAndCannotBeReplayed(){
        var user=User.builder().id(UUID.randomUUID()).accountEnabled(false).build();
        var activation=AccountActivation.builder().user(user).expiresAt(Instant.now().plusSeconds(60)).build();
        when(tokens.findForUpdate(anyString())).thenReturn(Optional.of(activation));
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        service.activate("token","Long-example-password-42");
        assertThat(user.getAccountEnabled()).isTrue();
        assertThat(encoder.matches("Long-example-password-42",user.getPasswordHash())).isTrue();
        assertThat(user.getCredentialVersion()).isEqualTo(1);
        assertThatThrownBy(()->service.activate("token","Other-password-42")).hasMessageContaining("invalid or expired");
    }
    @Test void expiredMissingAndOverlongUtf8PasswordsFailWithoutEnablingAccount(){
        assertThatThrownBy(()->service.activate("missing","Long-example-password-42")).hasMessageContaining("invalid or expired");
        var user=User.builder().id(UUID.randomUUID()).accountEnabled(false).build();
        var activation=AccountActivation.builder().user(user).expiresAt(Instant.now().minusSeconds(1)).build();
        when(tokens.findForUpdate(anyString())).thenReturn(Optional.of(activation));
        assertThatThrownBy(()->service.activate("expired","Long-example-password-42")).hasMessageContaining("invalid or expired");
        activation.setExpiresAt(Instant.now().plusSeconds(60));
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        assertThatThrownBy(()->service.activate("token","क".repeat(30))).hasMessageContaining("72 UTF-8");
        assertThat(user.getAccountEnabled()).isFalse();verify(users,never()).save(any());
    }
}
