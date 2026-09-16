package com.neml.badminton.dto;
import jakarta.validation.constraints.*; import java.util.*;
public class CricketDtos {
 public record PlayingXiRequest(@NotEmpty List<@NotNull UUID> playerIds,@NotNull UUID captainId,UUID wicketkeeperId,List<@NotNull UUID> wicketkeeperIds){}
 public record TossRequest(@NotNull UUID winnerTeamId,@NotBlank @Pattern(regexp="BAT|BOWL") String decision){}
 public record StartInningsRequest(@NotNull UUID battingTeamId){}
 public record DeliveryRequest(@NotBlank @Size(max=100) String idempotencyKey,@NotNull UUID strikerPlayerId,@NotNull UUID nonStrikerPlayerId,@NotNull UUID bowlerPlayerId,UUID wicketPlayerId,@Min(0)@Max(6) int batterRuns,
   @Min(0) int wides,@Min(0) int noBalls,@Min(0) int byes,@Min(0) int legByes,@Min(0) int penaltyRuns,
   boolean wicket,@Size(max=32) String dismissalType){}
 public record BatterStatsDto(UUID playerId,int runs,int balls,int fours,int sixes,boolean out){}
 public record BowlerStatsDto(UUID playerId,int runsConceded,int wickets,int legalBalls,String overs){}
 public record DeliveryDto(UUID id,int sequence,UUID strikerPlayerId,UUID nonStrikerPlayerId,UUID bowlerPlayerId,int batterRuns,int wides,int noBalls,int byes,int legByes,int penaltyRuns,boolean wicket,String dismissalType,boolean legalBall,int totalRuns){}
 public record InningsDto(UUID id,Long version,int inningsNumber,UUID battingTeamId,int runs,int wickets,int legalBalls,String overs,String status,Integer target,Double requiredRunRate,UUID nextStrikerPlayerId,UUID nextNonStrikerPlayerId,List<BatterStatsDto> batters,List<BowlerStatsDto> bowlers,List<DeliveryDto> deliveries){}
 public record MatchStateDto(UUID matchId,UUID tossWinnerTeamId,String tossDecision,String resultText,List<InningsDto> innings){}
 public record AdministrativeResultRequest(@NotBlank @Pattern(regexp="TEAM_WIN|TIE|NO_RESULT|ABANDONED")String outcome,UUID winnerTeamId,@NotBlank @Size(max=500)String reason){}
 public record DeliveryCorrectionRequest(@NotNull Long expectedInningsVersion,@NotBlank @Size(max=500)String reason){}
 public record TargetOverrideRequest(@NotNull @Min(1)Integer targetRuns,@NotBlank @Size(max=500)String reason){}
}
