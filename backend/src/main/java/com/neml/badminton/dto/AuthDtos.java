package com.neml.badminton.dto;

import com.neml.badminton.entity.Role;
import com.neml.badminton.entity.ChampionshipRoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, UserInfo user) {}

    public record ChampionshipAccess(String championshipId, ChampionshipRoleType role, String teamId) {}

    public record UserInfo(String id, String email, String fullName, Role role,
                           java.util.List<ChampionshipAccess> championships) {}
}
