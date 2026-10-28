package com.neml.badminton.dto;

import com.neml.badminton.entity.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;

public class MatchDtos {

    public record AdministrativeFixtureResultRequest(@NotNull UUID winnerTeamId,@NotBlank @Size(max=500)String reason) {}

    public record TeamRef(UUID id, String name, String shortCode, String primaryColor) {
        public static TeamRef from(Team t) {
            if (t == null) return null;
            return new TeamRef(t.getId(), t.getName(), t.getShortCode(), t.getPrimaryColor());
        }
    }

    public record PlayerRef(UUID id, String fullName, String gender, UUID teamId,String photoUrl) {
        public static PlayerRef from(Player p) {
            if (p == null) return null;
            return new PlayerRef(p.getId(), p.getFullName(), p.getGender().name(),
                    p.getTeam() == null ? null : p.getTeam().getId(),Dtos.PlayerDto.photoUrl(p));
        }
    }

    public record FormatDto(UUID id, FormatType formatType, Integer formatOrder,
                            String label, String shortLabel, Integer playersPerSide, String genderRule,
                            List<PlayerRef> sideAPlayers, List<PlayerRef> sideBPlayers,
                            Integer scoreA, Integer scoreB, TeamRef winner, Boolean completed) {
        public static FormatDto from(MatchFormat f) {
            return new FormatDto(
                    f.getId(), f.getFormatType(), f.getFormatOrder(),
                    f.getDisplayLabel()==null?MatchDtos.label(f.getFormatType()):f.getDisplayLabel(), f.getShortLabel()==null?MatchDtos.shortLabel(f.getFormatType()):f.getShortLabel(),
                    f.getPlayersPerSide()==null?MatchDtos.playersPerSide(f.getFormatType()):f.getPlayersPerSide(), f.getGenderRule()==null?MatchDtos.genderRule(f.getFormatType()):f.getGenderRule(),
                    f.getSideAPlayers().stream().map(PlayerRef::from).toList(),
                    f.getSideBPlayers().stream().map(PlayerRef::from).toList(),
                    f.getScoreA(), f.getScoreB(),
                    TeamRef.from(f.getWinnerTeam()), f.getCompleted());
        }
    }

    public record MatchDto(UUID id, Integer matchNumber, CompetitionStageType stageType,
                           Integer roundNumber, Integer legNumber, String fixtureKey,
                           Integer seedA, Integer seedB, TeamRef teamA, TeamRef teamB,
                           Instant scheduledAt, MatchStatus status, TeamRef winner,
                           Integer teamAFormatWins, Integer teamBFormatWins,
                           Integer teamAPoints, Integer teamBPoints, Integer resultRevision,
                           String venue, List<FormatDto> formats) {
        public static MatchDto from(Match m) {
            return new MatchDto(
                    m.getId(), m.getMatchNumber(), m.getStageType(), m.getRoundNumber(), m.getLegNumber(),
                    m.getFixtureKey(), m.getSeedA(), m.getSeedB(),
                    TeamRef.from(m.getTeamA()), TeamRef.from(m.getTeamB()),
                    m.getScheduledAt(), m.getStatus(), TeamRef.from(m.getWinnerTeam()),
                    m.getTeamAFormatWins(), m.getTeamBFormatWins(), m.getTeamAPoints(), m.getTeamBPoints(),
                    m.getResultRevision(),
                    m.getVenue(),
                    m.getFormats().stream()
                            .sorted((a, b) -> Integer.compare(a.getFormatOrder(), b.getFormatOrder()))
                            .map(FormatDto::from).toList());
        }
    }

    private static String label(FormatType type) {
        return switch (type) {
            case MENS_SINGLES -> "Men's Singles";
            case WOMENS_SINGLES -> "Women's Singles";
            case MENS_DOUBLES -> "Men's Doubles";
            case MIXED_DOUBLES -> "Mixed Doubles";
            case MENS_DOUBLES_TWO -> "Men's Doubles II";
        };
    }
    private static String shortLabel(FormatType type) {
        return switch (type) {
            case MENS_SINGLES -> "MS";
            case WOMENS_SINGLES -> "WS";
            case MENS_DOUBLES -> "MD";
            case MIXED_DOUBLES -> "XD";
            case MENS_DOUBLES_TWO -> "MD2";
        };
    }
    private static int playersPerSide(FormatType type) {
        return type == FormatType.MENS_SINGLES || type == FormatType.WOMENS_SINGLES ? 1 : 2;
    }
    private static String genderRule(FormatType type) {
        return switch (type) {
            case WOMENS_SINGLES -> "FEMALE";
            case MIXED_DOUBLES -> "MIXED";
            default -> "MALE";
        };
    }

    public record CreateMatchRequest(@NotNull UUID teamAId, @NotNull UUID teamBId,
                                     @NotNull Instant scheduledAt, @Size(max=255) String venue) {}

    public record AssignPlayersRequest(@NotNull List<UUID> sideAPlayerIds, @NotNull List<UUID> sideBPlayerIds) {}

    public record ReportFormatResultRequest(@NotNull @PositiveOrZero Integer scoreA,
                                            @NotNull @PositiveOrZero Integer scoreB) {}

    public record StandingDto(TeamRef team, Integer played, Integer won, Integer lost,
                              Integer formatWins, Integer formatLosses, Integer formatDiff,
                              Integer basePoints, Integer penalty, Integer totalPoints,
                              Double netRunRate,Integer rank, List<UUID> unplayedPlayerIds) {}

    public record FormatLeaderDto(FormatType formatType, TeamRef leadingTeam, Integer wins,
                                  List<TeamStat> allTeams) {
        public record TeamStat(TeamRef team, Integer wins) {}
    }

    public record TeamAnalysisDto(TeamRef team, Integer played, Integer won, Integer lost,
                                  Double winPct, Integer participationCount, Integer squadSize,
                                  Double participationPct, List<UUID> unplayedPlayerIds,
                                  FormatType strongestFormat, FormatType weakestFormat,
                                  List<FormatBreakdown> formatBreakdown,
                                  List<HeadToHead> headToHead) {
        public record FormatBreakdown(FormatType formatType, Integer won, Integer lost) {}
        public record HeadToHead(TeamRef opponent, Integer played, Integer won, Integer lost) {}
    }

    public record TopPerformerDto(PlayerRef player, TeamRef team, Integer matchesPlayed,
                                  Integer wins, Integer losses, Double winPct,
                                  Integer longestStreak, Integer totalPointsWon, Integer avgMargin) {}
}
