package com.neml.badminton.dto;

import com.neml.badminton.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

public class ChampionshipDtos {
    public record CreateChampionshipRequest(@NotBlank String name, @NotBlank String sportType,
                                            String roomCode, String passcode, Boolean isPublic,
                                            Integer maxSquadSize, BigDecimal purseLimit,
                                            String customRules) {}
    public record JoinRoomRequest(@NotBlank String roomCode, String passcode) {}
    public record AssignRoleRequest(@Email @NotBlank String email, @NotNull ChampionshipRoleType role,
                                    UUID teamId) {}
    public record ChampionshipDto(UUID id, String name, String sportType, String roomCode,
                                  Boolean isPublic, ChampionshipStatus status, UUID defaultAuctionId) {}
    public record JoinRoomResponse(String token, ChampionshipDto championship) {}

    public static ChampionshipDto from(Championship c, UUID auctionId) {
        return new ChampionshipDto(c.getId(), c.getName(), c.getSportType(), c.getRoomCode(),
                c.getIsPublic(), c.getStatus(), auctionId);
    }
}
