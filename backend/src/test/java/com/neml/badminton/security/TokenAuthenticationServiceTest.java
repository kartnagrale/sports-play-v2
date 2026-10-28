package com.neml.badminton.security;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class TokenAuthenticationServiceTest {
    final JwtService jwt=new JwtService("phase1-test-secret-012345678901234567890123456789",60000);
    final UserRepository users=mock(UserRepository.class);
    final TokenAuthenticationService service=new TokenAuthenticationService(jwt,users);
    @Test void correctlyFormedTokenSignedWithAnotherKeyIsRejectedBeforeAccountLookup(){
        var foreignSigner=new JwtService("untrusted-signing-key-012345678901234567890123456789",60000);
        String token=foreignSigner.generateToken(UUID.randomUUID().toString(),Map.of("cv",0));
        assertThatThrownBy(()->service.authenticate(token)).isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
        verifyNoInteractions(users);
    }
    @Test void revokedInactiveInvalidAndExpiredSessionsFail(){
        var user=User.builder().id(UUID.randomUUID()).role(Role.USER).build();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        String token=jwt.generateToken(user.getId().toString(),Map.of("cv",0));
        assertThat(service.authenticate(token).getPrincipal()).isSameAs(user);
        user.setCredentialVersion(1);
        assertThatThrownBy(()->service.authenticate(token)).hasMessageContaining("no longer valid");
        user.setCredentialVersion(0);user.setAccountEnabled(false);
        assertThatThrownBy(()->service.authenticate(token)).hasMessageContaining("no longer valid");
        assertThatThrownBy(()->service.authenticate("bad")).isInstanceOf(RuntimeException.class);
        var expired=new JwtService("phase1-test-secret-012345678901234567890123456789",-10000);
        assertThatThrownBy(()->service.authenticate(expired.generateToken(user.getId().toString(),Map.of("cv",0)))).isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
