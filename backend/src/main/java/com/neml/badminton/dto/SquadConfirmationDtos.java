package com.neml.badminton.dto;

import com.neml.badminton.entity.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.*;

public class SquadConfirmationDtos {
    public record SquadActionRequest(@Size(max = 500) String notes) {}

    public record PersonRef(UUID id, String fullName) {
        public static PersonRef from(User user) {
            return user == null ? null : new PersonRef(user.getId(), user.getFullName());
        }
    }

    public record SquadTeamDto(UUID teamId, String teamName, String shortCode, String primaryColor,
                               SquadConfirmationStatus status, boolean rosterValid, List<String> validationIssues,
                               int playerCount, int maleCount, int femaleCount, int requiredPlayers,
                               int requiredMale, int requiredFemale, PersonRef confirmedBy, Instant confirmedAt,
                               PersonRef lockedBy, Instant lockedAt, PersonRef reopenedBy, Instant reopenedAt,
                               String notes, Instant updatedAt) {}

    public record SquadSummaryDto(String phaseStatus, boolean auctionCompleted, boolean allSquadsLocked,
                                  int totalTeams, int readyTeams, int confirmedTeams, int lockedTeams,
                                  List<SquadTeamDto> teams) {}

    public record SquadEventDto(UUID id, UUID teamId, String teamName, SquadConfirmationAction action,
                                PersonRef actor, Instant occurredAt, String notes) {
        public static SquadEventDto from(SquadConfirmationEvent event) {
            return new SquadEventDto(event.getId(), event.getTeam().getId(), event.getTeam().getName(),
                    event.getAction(), PersonRef.from(event.getActor()), event.getOccurredAt(), event.getNotes());
        }
    }
}
