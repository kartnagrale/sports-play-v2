package com.neml.badminton.dto;

import com.neml.badminton.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

public class ChampionshipDtos {
    public record CreateChampionshipRequest(@NotBlank String name, @NotBlank String sportType,
                                            String roomCode, String passcode, Boolean isPublic,
                                            @NotBlank String adminFullName,
                                            @Email @NotBlank String adminEmail,
                                            @Pattern(regexp = "^[0-9+() -]{7,20}$") @NotBlank String adminMobile) {}
    public record ConfigureChampionshipRequest(@NotBlank String auctionName,
                                               @NotNull @Min(1) Integer maxSquadSize,
                                               @NotNull @DecimalMin("0.01") BigDecimal purseLimit,
                                               @NotNull @Min(0) Integer minMale,
                                               @NotNull @Min(0) Integer minFemale,
                                               @NotNull @DecimalMin("0.01") BigDecimal playerBasePrice,
                                               @NotNull @DecimalMin("0.01") BigDecimal bidIncrement,
                                               @NotNull @Min(5) @Max(300) Integer timerSeconds,
                                               String customRules) {}
    public record ChampionshipSettingsDto(UUID championshipId, ChampionshipStatus status,
                                          String auctionName, Integer maxSquadSize,
                                          BigDecimal purseLimit, Integer minMale, Integer minFemale,
                                          BigDecimal playerBasePrice, BigDecimal bidIncrement,
                                          Integer timerSeconds, String customRules, UUID auctionId) {}
    public record CreateTeamCaptainRequest(@NotBlank String teamName, @NotBlank @Size(max=12) String shortCode,
                                           String primaryColor, String logoUrl,
                                           @NotBlank String captainFullName,
                                           @Email @NotBlank String captainEmail,
                                           @Pattern(regexp = "^[0-9+() -]{7,20}$") @NotBlank String captainMobile) {}
    public record ProvisionedTeamDto(UUID teamId, String teamName, String shortCode,
                                    UUID captainUserId, String captainName, String captainEmail,
                                    String captainMobile, String temporaryPassword, boolean messageQueued) {}
    public record JoinRoomRequest(@NotBlank String roomCode, String passcode) {}
    public record AssignRoleRequest(@Email @NotBlank String email, @NotNull ChampionshipRoleType role,
                                    UUID teamId) {}
    public record ChampionshipDto(UUID id, String name, String sportType, String roomCode,
                                  Boolean isPublic, ChampionshipStatus status, UUID defaultAuctionId) {}
    public record ProvisionedAdminDto(UUID userId, String fullName, String email, String mobile, String temporaryPassword,
                                      boolean messageQueued) {}
    public record CreateChampionshipResponse(ChampionshipDto championship, ProvisionedAdminDto admin) {}
    public record JoinRoomResponse(String token, ChampionshipDto championship) {}

    public static ChampionshipDto from(Championship c, UUID auctionId) {
        return new ChampionshipDto(c.getId(), c.getName(), c.getSportType(), c.getRoomCode(),
                c.getIsPublic(), c.getStatus(), auctionId);
    }
}
