package com.neml.badminton.security;

import com.neml.badminton.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
public class TokenAuthenticationService {
    private final JwtService jwt; private final UserRepository users;
    public TokenAuthenticationService(JwtService jwt,UserRepository users){this.jwt=jwt;this.users=users;}
    public record Session(Instant expiresAt,Integer credentialVersion) {}
    public UsernamePasswordAuthenticationToken authenticate(String token) {
        var claims=jwt.parse(token);
        Object principal;String role;Integer version=null;
        if("viewer".equals(claims.get("token_type",String.class))){
            principal=new ViewerPrincipal(UUID.fromString(claims.get("championship_id",String.class)));role="VIEWER";
        }else{
            var user=users.findById(UUID.fromString(claims.getSubject())).orElseThrow(()->new JwtException("Unknown account"));
            Number supplied=claims.get("cv",Number.class);
            if(!Boolean.TRUE.equals(user.getAccountEnabled())||supplied==null||supplied.intValue()!=user.getCredentialVersion())
                throw new JwtException("Account session is no longer valid");
            principal=user;role=user.getRole().name();version=user.getCredentialVersion();
        }
        if(claims.getExpiration()==null)throw new JwtException("Session expiry required");
        var auth=new UsernamePasswordAuthenticationToken(principal,null,List.of(new SimpleGrantedAuthority("ROLE_"+role)));
        auth.setDetails(new Session(claims.getExpiration().toInstant(),version));return auth;
    }
}
