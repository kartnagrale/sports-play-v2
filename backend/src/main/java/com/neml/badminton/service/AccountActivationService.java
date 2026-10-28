package com.neml.badminton.service;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class AccountActivationService {
    private final AccountActivationRepository activations;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Duration lifetime;
    private final SecureRandom random=new SecureRandom();
    public AccountActivationService(AccountActivationRepository activations,UserRepository users,
            PasswordEncoder encoder,@Value("${app.auth.activation-lifetime:PT30M}") Duration lifetime) {
        if(lifetime.isNegative()||lifetime.isZero()||lifetime.compareTo(Duration.ofHours(24))>0)
            throw new IllegalArgumentException("Activation lifetime must be positive and at most 24 hours");
        this.activations=activations;this.users=users;this.encoder=encoder;this.lifetime=lifetime;
    }
    public record Invitation(String setupPath,Instant expiresAt) {}

    @Transactional
    public Invitation issue(User user) {
        user=users.findByIdForUpdate(user.getId()).orElseThrow(this::invalid);
        if(Boolean.TRUE.equals(user.getAccountEnabled()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Account is already active");
        user.setCredentialVersion(user.getCredentialVersion()+1);users.save(user);
        byte[] bytes=new byte[32];random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires=Instant.now().plus(lifetime);
        activations.save(AccountActivation.builder().user(user).tokenHash(hash(token)).expiresAt(expires)
                .credentialVersion(user.getCredentialVersion()).build());
        // A fragment keeps the secret out of HTTP request URLs/referrer logs.
        return new Invitation("/activate#token="+token,expires);
    }
    @Transactional
    public void activate(String token,String password) {
        var activation=activations.findForUpdate(hash(token)).orElseThrow(this::invalid);
        if(activation.getUsedAt()!=null||!activation.getExpiresAt().isAfter(Instant.now()))throw invalid();
        User user=users.findByIdForUpdate(activation.getUser().getId()).orElseThrow(this::invalid);
        if(Boolean.TRUE.equals(user.getAccountEnabled())||!Objects.equals(user.getCredentialVersion(),activation.getCredentialVersion()))throw invalid();
        // BCrypt operates on bytes, not Java characters. Never silently truncate passwords.
        if(password.length()<12||password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a password of at least 12 characters and at most 72 UTF-8 bytes");
        user.setPasswordHash(encoder.encode(password));user.setAccountEnabled(true);
        user.setCredentialVersion(user.getCredentialVersion()+1);
        activation.setUsedAt(Instant.now());users.save(user);activations.save(activation);
    }
    public static String hash(String token) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Setup link is invalid or expired");}
}
