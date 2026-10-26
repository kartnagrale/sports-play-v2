package com.neml.badminton.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neml.badminton.dto.SquadConfirmationDtos.*;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SquadConfirmationService {
    private final ChampionshipRepository championships;
    private final TeamRepository teams;
    private final PlayerRepository players;
    private final TournamentSettingsRepository settings;
    private final AuctionRepository auctions;
    private final MatchRepository matches;
    private final SquadConfirmationRepository confirmations;
    private final SquadConfirmationEventRepository events;
    private final ObjectMapper objectMapper;
    private final CompetitionConfigurationRepository competitionConfigurations;
    private final RosterQuotaRuleRepository quotaRules;
    private final ConfigurationSectionService configurationSections;
    private final AuctionConfigurationRepository auctionConfigurations;

    public SquadConfirmationService(ChampionshipRepository championships, TeamRepository teams,
            PlayerRepository players, TournamentSettingsRepository settings, AuctionRepository auctions,
            MatchRepository matches, SquadConfirmationRepository confirmations,
            SquadConfirmationEventRepository events, ObjectMapper objectMapper,
            CompetitionConfigurationRepository competitionConfigurations,RosterQuotaRuleRepository quotaRules,ConfigurationSectionService configurationSections,AuctionConfigurationRepository auctionConfigurations) {
        this.championships = championships; this.teams = teams; this.players = players;
        this.settings = settings; this.auctions = auctions; this.matches = matches;
        this.confirmations = confirmations; this.events = events; this.objectMapper = objectMapper;
        this.competitionConfigurations=competitionConfigurations;this.quotaRules=quotaRules;
        this.configurationSections=configurationSections;
        this.auctionConfigurations=auctionConfigurations;
    }

    @Transactional
    public SquadSummaryDto summary(UUID championshipId) {
        championship(championshipId);
        TournamentSettings rules = rules(championshipId);
        List<Team> championshipTeams = teams.findAllByChampionshipIdOrderByName(championshipId);
        Map<UUID, SquadConfirmation> state = confirmations.findAllByChampionshipId(championshipId).stream()
                .collect(Collectors.toMap(item -> item.getTeam().getId(), Function.identity()));
        Map<UUID, List<Player>> roster = players.findAllByChampionshipIdOrderByAuctionOrderAsc(championshipId).stream()
                .filter(player -> player.getTeam() != null)
                .collect(Collectors.groupingBy(player -> player.getTeam().getId()));

        List<SquadTeamDto> result = championshipTeams.stream()
                .map(team -> dto(team, state.get(team.getId()), validate(championshipId,roster.getOrDefault(team.getId(), List.of()), rules)))
                .toList();
        boolean auctionCompleted = !auctionEnabled(championshipId)||auctions.existsByChampionshipIdAndStatus(championshipId, AuctionStatus.COMPLETED);
        int ready = (int) result.stream().filter(SquadTeamDto::rosterValid).count();
        int confirmed = (int) result.stream().filter(item -> item.status() == SquadConfirmationStatus.CONFIRMED).count();
        int locked = (int) result.stream().filter(item -> item.status() == SquadConfirmationStatus.LOCKED).count();
        boolean allLocked = !result.isEmpty() && locked == result.size();
        String phase = allLocked ? "COMPLETED" : (confirmed > 0 || locked > 0 ? "IN_PROGRESS" :
                (auctionCompleted && ready == result.size() && !result.isEmpty() ? "READY" : "NOT_READY"));
        return new SquadSummaryDto(phase, auctionCompleted, allLocked, result.size(), ready, confirmed, locked, result);
    }

    @Transactional
    public SquadSummaryDto confirm(UUID championshipId, UUID teamId, User actor, SquadActionRequest request) {
        if (auctionEnabled(championshipId)&&!auctions.existsByChampionshipIdAndStatus(championshipId, AuctionStatus.COMPLETED)) {
            conflict("Complete the auction before confirming squads");
        }
        Team team = team(championshipId, teamId);
        Championship championship=team.getChampionship();
        championship.setLifecyclePhase(ChampionshipPhase.SQUAD_CONFIRMATION);
        championships.save(championship);
        RosterValidation validation = validation(championshipId, teamId);
        if (!validation.valid()) conflict(String.join("; ", validation.issues()));
        SquadConfirmation confirmation = confirmations.findForUpdate(championshipId, teamId)
                .orElseGet(() -> SquadConfirmation.builder().championship(team.getChampionship()).team(team).build());
        if (confirmation.getStatus() == SquadConfirmationStatus.LOCKED) conflict("This squad is already locked");
        if (confirmation.getStatus() == SquadConfirmationStatus.CONFIRMED) conflict("This squad is already confirmed");
        Instant now = Instant.now();
        confirmation.setStatus(SquadConfirmationStatus.CONFIRMED);
        confirmation.setConfirmedBy(actor); confirmation.setConfirmedAt(now);
        confirmation.setLockedBy(null); confirmation.setLockedAt(null);
        confirmation.setNotes(notes(request));
        confirmation = confirmations.save(confirmation);
        configurationSections.lock(championshipId,"TEAMS_AND_ROSTERS",actor);
        record(confirmation, SquadConfirmationAction.CONFIRMED, actor, confirmation.getNotes(), validation.players());
        return summary(championshipId);
    }

    @Transactional
    public SquadSummaryDto lock(UUID championshipId, UUID teamId, User actor, SquadActionRequest request) {
        SquadConfirmation confirmation = confirmations.findForUpdate(championshipId, teamId)
                .orElseThrow(() -> conflictException("The captain must confirm this squad first"));
        if (confirmation.getStatus() != SquadConfirmationStatus.CONFIRMED) {
            conflict(confirmation.getStatus() == SquadConfirmationStatus.LOCKED ? "This squad is already locked" : "The captain must confirm this squad first");
        }
        RosterValidation validation = validation(championshipId, teamId);
        if (!validation.valid()) conflict(String.join("; ", validation.issues()));
        confirmation.setStatus(SquadConfirmationStatus.LOCKED);
        confirmation.setLockedBy(actor); confirmation.setLockedAt(Instant.now());
        if (request != null && request.notes() != null && !request.notes().isBlank()) confirmation.setNotes(request.notes().trim());
        confirmations.save(confirmation);
        record(confirmation, SquadConfirmationAction.LOCKED, actor, confirmation.getNotes(), validation.players());
        return summary(championshipId);
    }

    @Transactional
    public SquadSummaryDto lockAll(UUID championshipId, User actor, SquadActionRequest request) {
        List<Team> championshipTeams = teams.findAllByChampionshipIdOrderByName(championshipId);
        if (championshipTeams.isEmpty()) conflict("Create teams before locking squads");
        for (Team team : championshipTeams) {
            SquadConfirmation confirmation = confirmations.findForUpdate(championshipId, team.getId())
                    .orElseThrow(() -> conflictException(team.getName() + " has not been confirmed by its captain"));
            if (confirmation.getStatus() == SquadConfirmationStatus.DRAFT) conflict(team.getName() + " has not been confirmed by its captain");
            if (confirmation.getStatus() == SquadConfirmationStatus.LOCKED) continue;
            RosterValidation validation = validation(championshipId, team.getId());
            if (!validation.valid()) conflict(team.getName() + ": " + String.join("; ", validation.issues()));
            confirmation.setStatus(SquadConfirmationStatus.LOCKED);
            confirmation.setLockedBy(actor); confirmation.setLockedAt(Instant.now());
            if (request != null && request.notes() != null && !request.notes().isBlank()) confirmation.setNotes(request.notes().trim());
            confirmations.save(confirmation);
            record(confirmation, SquadConfirmationAction.LOCKED, actor, confirmation.getNotes(), validation.players());
        }
        return summary(championshipId);
    }

    @Transactional
    public SquadSummaryDto reopen(UUID championshipId, UUID teamId, User actor, SquadActionRequest request) {
        if (!matches.findAllByChampionshipIdOrderByMatchNumberAsc(championshipId).isEmpty()) {
            conflict("Squads cannot be reopened after league fixtures have been created");
        }
        SquadConfirmation confirmation = confirmations.findForUpdate(championshipId, teamId)
                .orElseThrow(() -> conflictException("Squad confirmation not found"));
        if (confirmation.getStatus() == SquadConfirmationStatus.DRAFT) conflict("This squad is already open");
        RosterValidation validation = validation(championshipId, teamId);
        confirmation.setStatus(SquadConfirmationStatus.DRAFT);
        confirmation.setReopenedBy(actor); confirmation.setReopenedAt(Instant.now());
        confirmation.setLockedBy(null); confirmation.setLockedAt(null);
        confirmation.setConfirmedBy(null); confirmation.setConfirmedAt(null);
        confirmation.setNotes(notes(request));
        confirmations.save(confirmation);
        record(confirmation, SquadConfirmationAction.REOPENED, actor, confirmation.getNotes(), validation.players());
        return summary(championshipId);
    }

    @Transactional
    public List<SquadEventDto> history(UUID championshipId) {
        championship(championshipId);
        return events.findTop50ByChampionshipIdOrderByOccurredAtDesc(championshipId).stream().map(SquadEventDto::from).toList();
    }

    @Transactional
    public void assertLeagueReady(UUID championshipId) {
        List<Team> championshipTeams = teams.findAllByChampionshipIdOrderByName(championshipId);
        long locked = confirmations.findAllByChampionshipId(championshipId).stream()
                .filter(item -> item.getStatus() == SquadConfirmationStatus.LOCKED).count();
        if (championshipTeams.isEmpty() || locked != championshipTeams.size()) {
            conflict("Every team squad must be confirmed and locked before league matches can be created");
        }
    }

    public void assertTeamLocked(UUID championshipId, UUID teamId) {
        SquadConfirmation confirmation = confirmations.findByChampionshipIdAndTeamId(championshipId, teamId)
                .orElseThrow(() -> conflictException("The team squad is not confirmed"));
        if (confirmation.getStatus() != SquadConfirmationStatus.LOCKED)
            conflict("Only players from locked squads may be assigned to fixtures");
    }

    @Transactional
    public void assertRosterMutable(UUID championshipId) {
        if (confirmations.existsByChampionshipIdAndStatusIn(championshipId,
                List.of(SquadConfirmationStatus.CONFIRMED, SquadConfirmationStatus.LOCKED))) {
            conflict("Squad changes are blocked after confirmation starts; ask the championship admin to reopen the squad first");
        }
    }

    private RosterValidation validation(UUID championshipId, UUID teamId) {
        return validate(championshipId,players.findAllByChampionshipIdOrderByAuctionOrderAsc(championshipId).stream()
                .filter(player -> player.getTeam() != null && player.getTeam().getId().equals(teamId)).toList(), rules(championshipId));
    }
    private boolean auctionEnabled(UUID championshipId){return auctionConfigurations.findFirstByChampionshipIdOrderByConfigurationVersionDesc(championshipId).map(AuctionConfiguration::getEnabled).orElse(true);}

    private RosterValidation validate(UUID championshipId,List<Player> roster, TournamentSettings rules) {
        List<String> issues = new ArrayList<>();
        int male = (int) roster.stream().filter(player -> player.getGender() == Gender.MALE).count();
        int female = (int) roster.stream().filter(player -> player.getGender() == Gender.FEMALE).count();
        CompetitionConfiguration configuration=competitionConfigurations.findByChampionshipId(championshipId).orElse(null);
        int minimum=configuration==null?rules.getMaxSquadSize():configuration.getMinimumRosterSize();
        int maximum=configuration==null?rules.getMaxSquadSize():configuration.getMaximumRosterSize();
        if (roster.size()<minimum||roster.size()>maximum) issues.add("Squad must contain between " + minimum+" and "+maximum+" players");
        if (male < rules.getMinMale()) issues.add("Squad requires at least " + rules.getMinMale() + " male players");
        if (female < rules.getMinFemale()) issues.add("Squad requires at least " + rules.getMinFemale() + " female players");
        if (roster.stream().anyMatch(player -> player.getStatus() != PlayerStatus.SOLD)) issues.add("Every squad player must have SOLD status");
        for(RosterQuotaRule quota:quotaRules.findAllByChampionshipIdAndEnabledTrue(championshipId)){
            if(!"SQUAD".equalsIgnoreCase(quota.getAppliesTo())) continue;
            long count=switch(quota.getCategoryType().toUpperCase(Locale.ROOT)){
                case "GENDER"->roster.stream().filter(p->p.getGender().name().equalsIgnoreCase(quota.getCategoryValue())).count();
                case "SKILL_LEVEL","PLAYER_CATEGORY"->roster.stream().filter(p->p.getSkillLevel()!=null&&p.getSkillLevel().equalsIgnoreCase(quota.getCategoryValue())).count();
                default->0;
            };
            if((quota.getMinimumCount()!=null&&count<quota.getMinimumCount())||(quota.getMaximumCount()!=null&&count>quota.getMaximumCount()))
                issues.add(quota.getValidationMessage()==null||quota.getValidationMessage().isBlank()?quota.getRuleCode()+" quota is not satisfied":quota.getValidationMessage());
        }
        return new RosterValidation(issues.isEmpty(), List.copyOf(issues), List.copyOf(roster), male, female);
    }

    private SquadTeamDto dto(Team team, SquadConfirmation confirmation, RosterValidation validation) {
        TournamentSettings rules = rules(team.getChampionship().getId());
        SquadConfirmationStatus status = confirmation == null ? SquadConfirmationStatus.DRAFT : confirmation.getStatus();
        return new SquadTeamDto(team.getId(), team.getName(), team.getShortCode(), team.getPrimaryColor(), status,
                validation.valid(), validation.issues(), validation.players().size(), validation.male(), validation.female(),
                rules.getMaxSquadSize(), rules.getMinMale(), rules.getMinFemale(),
                confirmation == null ? null : PersonRef.from(confirmation.getConfirmedBy()), confirmation == null ? null : confirmation.getConfirmedAt(),
                confirmation == null ? null : PersonRef.from(confirmation.getLockedBy()), confirmation == null ? null : confirmation.getLockedAt(),
                confirmation == null ? null : PersonRef.from(confirmation.getReopenedBy()), confirmation == null ? null : confirmation.getReopenedAt(),
                confirmation == null ? null : confirmation.getNotes(), confirmation == null ? null : confirmation.getUpdatedAt());
    }

    private void record(SquadConfirmation confirmation, SquadConfirmationAction action, User actor, String notes, List<Player> roster) {
        events.save(SquadConfirmationEvent.builder().championship(confirmation.getChampionship()).team(confirmation.getTeam())
                .confirmation(confirmation).action(action).actor(actor).notes(notes).rosterSnapshot(snapshot(roster)).build());
    }

    private String snapshot(List<Player> roster) {
        try {
            return objectMapper.writeValueAsString(roster.stream().map(player -> Map.of(
                    "playerId", player.getId().toString(), "fullName", player.getFullName(),
                    "gender", player.getGender().name(), "status", player.getStatus().name())).toList());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to record squad snapshot", exception);
        }
    }

    private String notes(SquadActionRequest request) {
        return request == null || request.notes() == null || request.notes().isBlank() ? null : request.notes().trim();
    }
    private Championship championship(UUID id) { return championships.findById(id).orElseThrow(() -> notFound("Championship")); }
    private Team team(UUID cid, UUID teamId) { return teams.findByIdAndChampionshipId(teamId, cid).orElseThrow(() -> notFound("Team")); }
    private TournamentSettings rules(UUID cid) { return settings.findByChampionshipId(cid).orElseThrow(() -> notFound("Tournament settings")); }
    private void conflict(String message) { throw conflictException(message); }
    private ResponseStatusException conflictException(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private ResponseStatusException notFound(String value) { return new ResponseStatusException(HttpStatus.NOT_FOUND, value + " not found"); }
    private record RosterValidation(boolean valid, List<String> issues, List<Player> players, int male, int female) {}
}
