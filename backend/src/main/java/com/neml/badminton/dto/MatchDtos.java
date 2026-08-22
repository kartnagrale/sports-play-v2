package com.neml.badminton.dto;

import com.neml.badminton.entity.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;

public class MatchDtos {

    public record TeamRef(UUID id, String name, String shortCode, String primaryColor) {
        public static TeamRef from(Team t) {
            if (t == null) return null;
            return new TeamRef(t.getId(), t.getName(), t.getShortCode(), t.getPrimaryColor());
        }
    }

    public record PlayerRef(UUID id, String fullName, String gender, UUID teamId) {
        public static PlayerRef from(Player p) {
            if (p == null) return null;
            return new PlayerRef(p.getId(), p.getFullName(), p.getGender().name(),
                    p.getTeam() == null ? null : p.getTeam().getId());
        }
    }

    public record FormatDto(UUID id, FormatType formatType, Integer formatOrder,
                            List<PlayerRef> sideAPlayers, List<PlayerRef> sideBPlayers,
                            Integer scoreA, Integer scoreB, TeamRef winner, Boolean completed) {
        public static FormatDto from(MatchFormat f) {
            return new FormatDto(
                    f.getId(), f.getFormatType(), f.getFormatOrder(),
                    f.getSideAPlayers().stream().map(PlayerRef::from).toList(),
                    f.getSideBPlayers().stream().map(PlayerRef::from).toList(),
                    f.getScoreA(), f.getScoreB(),
                    TeamRef.from(f.getWinnerTeam()), f.getCompleted());
        }
    }

    public record MatchDto(UUID id, Integer matchNumber, TeamRef teamA, TeamRef teamB,
                           Instant scheduledAt, MatchStatus status, TeamRef winner,
                           Integer teamAFormatWins, Integer teamBFormatWins,
                           String venue, List<FormatDto> formats) {
        public static MatchDto from(Match m) {
            return new MatchDto(
                    m.getId(), m.getMatchNumber(),
                    TeamRef.from(m.getTeamA()), TeamRef.from(m.getTeamB()),
                    m.getScheduledAt(), m.getStatus(), TeamRef.from(m.getWinnerTeam()),
                    m.getTeamAFormatWins(), m.getTeamBFormatWins(),
                    m.getVenue(),
                    m.getFormats().stream()
                            .sorted((a, b) -> Integer.compare(a.getFormatOrder(), b.getFormatOrder()))
                            .map(FormatDto::from).toList());
        }
    }

    public record CreateMatchRequest(@NotNull UUID teamAId, @NotNull UUID teamBId,
                                     @NotNull Instant scheduledAt, @Size(max=255) String venue) {}

    public record AssignPlayersRequest(@NotNull List<UUID> sideAPlayerIds, @NotNull List<UUID> sideBPlayerIds) {}

    public record ReportFormatResultRequest(@NotNull @PositiveOrZero Integer scoreA,
                                            @NotNull @PositiveOrZero Integer scoreB) {}

    public record StandingDto(TeamRef team, Integer played, Integer won, Integer lost,
                              Integer formatWins, Integer formatLosses, Integer formatDiff,
                              Integer basePoints, Integer penalty, Integer totalPoints,
                              Integer rank, List<UUID> unplayedPlayerIds) {}

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
