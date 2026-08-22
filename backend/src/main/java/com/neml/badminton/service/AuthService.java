package com.neml.badminton.service;

import com.neml.badminton.dto.AuthDtos;
import com.neml.badminton.entity.User;
import com.neml.badminton.repository.UserRepository;
import com.neml.badminton.repository.ChampionshipRoleRepository;
import com.neml.badminton.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ChampionshipRoleRepository championshipRoleRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       ChampionshipRoleRepository championshipRoleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.championshipRoleRepository = championshipRoleRepository;
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("global_role", user.getRole().name());
        String token = jwtService.generateToken(user.getId().toString(), claims);
        AuthDtos.UserInfo info = new AuthDtos.UserInfo(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(), accesses(user)
        );
        return new AuthDtos.AuthResponse(token, info);
    }

    public AuthDtos.UserInfo me(User user) {
        return new AuthDtos.UserInfo(
                user.getId().toString(), user.getEmail(), user.getFullName(), user.getRole(), accesses(user)
        );
    }

    private java.util.List<AuthDtos.ChampionshipAccess> accesses(User user) {
        return championshipRoleRepository.findAllByUserId(user.getId()).stream()
                .map(r -> new AuthDtos.ChampionshipAccess(r.getChampionship().getId().toString(), r.getRole(),
                        r.getTeam() == null ? null : r.getTeam().getId().toString()))
                .toList();
    }
}
