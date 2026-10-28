package com.neml.badminton.dto;

import com.neml.badminton.entity.Role;
import com.neml.badminton.entity.ChampionshipRoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record LoginRequest(@Email @NotBlank @Size(max=254) String email, @NotBlank @Size(max=72) String password) {}
    public record ActivateRequest(@NotBlank @Size(min=43,max=43) String token,
                                  @NotBlank @Size(min=12,max=72) String password) {}

    public record AuthResponse(String token, UserInfo user) {}

    public record ChampionshipAccess(String championshipId, ChampionshipRoleType role, String teamId) {}

    public record UserInfo(String id, String email, String fullName, Role role,
                           java.util.List<ChampionshipAccess> championships) {}
}
