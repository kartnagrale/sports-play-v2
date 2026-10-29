package com.neml.badminton.dto;

import com.neml.badminton.entity.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;

public class Dtos {

    public record TeamDto(UUID id, String name, String shortCode, String logoUrl, String primaryColor,
                          BigDecimal purseTotal, BigDecimal purseRemaining,
                          Integer maleCount, Integer femaleCount, Integer totalPlayers, Integer matchPoints) {
        public static TeamDto from(Team t) {
            int total = (t.getMaleCount() == null ? 0 : t.getMaleCount()) +
                        (t.getFemaleCount() == null ? 0 : t.getFemaleCount());
            return new TeamDto(t.getId(), t.getName(), t.getShortCode(), t.getLogoUrl(), t.getPrimaryColor(),
                    t.getPurseTotal(), t.getPurseRemaining(), t.getMaleCount(), t.getFemaleCount(), total,
                    t.getMatchPoints() == null ? 0 : t.getMatchPoints());
        }
    }

    public record PlayerDto(UUID id, String fullName, Gender gender, BigDecimal basePrice,
                            BigDecimal soldPrice, PlayerStatus status, UUID teamId, String teamName,
                            String skillLevel, Integer auctionOrder, String playerReference, String photoUrl,UUID captainUserId) {
        public static String photoUrl(Player p){return p.getPhotoKey()==null?null:"/championships/"+p.getChampionship().getId()+"/players/"+p.getId()+"/photo?version="+p.getPhotoKey();}
        public static PlayerDto from(Player p) {
            return new PlayerDto(
                    p.getId(), p.getFullName(), p.getGender(), p.getBasePrice(),
                    p.getSoldPrice(), p.getStatus(),
                    p.getTeam() == null ? null : p.getTeam().getId(),
                    p.getTeam() == null ? null : p.getTeam().getName(),
                    p.getSkillLevel(), p.getAuctionOrder(), p.getPlayerReference(),photoUrl(p),p.getCaptainUserId()
            );
        }
    }

    public record BidDto(UUID id, UUID playerId, String playerName, UUID teamId, String teamName,
                         BigDecimal amount, Instant createdAt, Boolean active, String playerPhotoUrl) {
        public static BidDto from(Bid b) {
            return new BidDto(b.getId(), b.getPlayer().getId(), b.getPlayer().getFullName(),
                    b.getTeam().getId(), b.getTeam().getName(),
                    b.getAmount(), b.getCreatedAt(), b.getActive(),PlayerDto.photoUrl(b.getPlayer()));
        }
    }

    public record AuctionStateDto(AuctionStatus status, PlayerDto currentPlayer,
                                  BidDto highestBid, List<BidDto> bidHistory,
                                  List<TeamDto> teams, Integer remainingPlayers,
                                  java.time.Instant bidDeadline, Integer timerSeconds,
                                  BigDecimal bidIncrement, Integer maxSquadSize,
                                  Integer minMale, Integer minFemale,
                                  Long revision, List<BidDto> teamBids,BigDecimal minimumBid,BigDecimal maximumBid,Integer maximumPremiumPurchases,
                                  List<com.neml.badminton.service.AuctionRuleEvaluator.TeamEligibility> teamEligibility) {}

    public record PlaceBidRequest(@NotNull UUID playerId, @NotNull UUID teamId,
                                  @NotNull @DecimalMin(value = "0.01") @Digits(integer=12,fraction=2) BigDecimal amount) {}

    public record SellRequest(UUID playerId, UUID teamId, BigDecimal amount) {}

    public record NextPlayerRequest(@NotNull UUID playerId) {}

    public record SetStatusRequest(@NotNull AuctionStatus status) {}

    public record UpdateBasePriceRequest(@NotNull @DecimalMin(value = "0.01") @Digits(integer=12,fraction=2) BigDecimal basePrice) {}
}
