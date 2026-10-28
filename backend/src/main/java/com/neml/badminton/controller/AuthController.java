package com.neml.badminton.controller;

import com.neml.badminton.dto.AuthDtos;
import com.neml.badminton.entity.User;
import com.neml.badminton.security.JwtAuthFilter;
import com.neml.badminton.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.neml.badminton.service.AccountActivationService;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountActivationService activations;
    private final boolean secureCookie;
    private final int maxAge;

    public AuthController(AuthService authService,
                          AccountActivationService activations,
                          @Value("${app.auth.cookie-secure:true}") boolean secureCookie,
                          @Value("${app.jwt.expiration-ms:28800000}") long expirationMs) {
        this.authService = authService;
        this.activations=activations;
        this.secureCookie = secureCookie;
        this.maxAge=Math.toIntExact(expirationMs/1000);
    }

    @GetMapping("/csrf")
    public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}

    @PostMapping("/activate")
    public ResponseEntity<Void> activate(@Valid @RequestBody AuthDtos.ActivateRequest req){
        activations.activate(req.token(),req.password());return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String,AuthDtos.UserInfo>> login(@Valid @RequestBody AuthDtos.LoginRequest req,
                                                       HttpServletResponse response) {
        AuthDtos.AuthResponse auth = authService.login(req);
        response.addHeader("Set-Cookie", buildCookie(auth.token(), maxAge));
        return ResponseEntity.ok(Map.of("user",auth.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response,@AuthenticationPrincipal User user) {
        authService.logout(user);
        response.addHeader("Set-Cookie", buildCookie("", 0));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.UserInfo> me(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(authService.me(user));
    }

    private String buildCookie(String value, int maxAgeSeconds) {
        String cookie = String.format("%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",
                JwtAuthFilter.COOKIE_NAME, value, maxAgeSeconds);
        return secureCookie ? cookie + "; Secure" : cookie;
    }
}
