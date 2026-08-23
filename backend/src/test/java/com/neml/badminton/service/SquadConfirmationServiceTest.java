package com.neml.badminton.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neml.badminton.dto.SquadConfirmationDtos.SquadActionRequest;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SquadConfirmationServiceTest {
    private final ChampionshipRepository championships = mock(ChampionshipRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final PlayerRepository players = mock(PlayerRepository.class);
    private final TournamentSettingsRepository settings = mock(TournamentSettingsRepository.class);
    private final AuctionRepository auctions = mock(AuctionRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final SquadConfirmationRepository confirmations = mock(SquadConfirmationRepository.class);
    private final SquadConfirmationEventRepository events = mock(SquadConfirmationEventRepository.class);
    private SquadConfirmationService service;

    @BeforeEach
    void setUp() {
        service = new SquadConfirmationService(championships, teams, players, settings, auctions,
                matches, confirmations, events, new ObjectMapper());
    }

    @Test
    void captainCanConfirmOnlyACompleteValidSoldRoster() {
        UUID championshipId = UUID.randomUUID();
        Championship championship = Championship.builder().id(championshipId).name("Badminton").sportType("BADMINTON").build();
        Team team = Team.builder().id(UUID.randomUUID()).championship(championship).name("Aces").shortCode("ACE")
                .primaryColor("#7CFF6B").build();
        User captain = User.builder().id(UUID.randomUUID()).fullName("Captain").build();
        TournamentSettings rules = TournamentSettings.builder().championship(championship)
                .maxSquadSize(12).minMale(7).minFemale(3).build();
        List<Player> roster = roster(championship, team, 9, 3);
        AtomicReference<SquadConfirmation> saved = new AtomicReference<>();

        when(auctions.existsByChampionshipIdAndStatus(championshipId, AuctionStatus.COMPLETED)).thenReturn(true);
        when(championships.findById(championshipId)).thenReturn(Optional.of(championship));
        when(teams.findByIdAndChampionshipId(team.getId(), championshipId)).thenReturn(Optional.of(team));
        when(teams.findAllByChampionshipIdOrderByName(championshipId)).thenReturn(List.of(team));
        when(settings.findByChampionshipId(championshipId)).thenReturn(Optional.of(rules));
        when(players.findAllByChampionshipIdOrderByAuctionOrderAsc(championshipId)).thenReturn(roster);
        when(confirmations.findForUpdate(championshipId, team.getId())).thenReturn(Optional.empty());
        when(confirmations.save(any())).thenAnswer(invocation -> { saved.set(invocation.getArgument(0)); return saved.get(); });
        when(confirmations.findAllByChampionshipId(championshipId)).thenAnswer(invocation -> saved.get() == null ? List.of() : List.of(saved.get()));

        var result = service.confirm(championshipId, team.getId(), captain, new SquadActionRequest("Reviewed"));

        assertThat(result.phaseStatus()).isEqualTo("IN_PROGRESS");
        assertThat(result.teams().get(0).status()).isEqualTo(SquadConfirmationStatus.CONFIRMED);
        assertThat(saved.get().getConfirmedBy()).isEqualTo(captain);
        verify(events).save(argThat(event -> event.getAction() == SquadConfirmationAction.CONFIRMED
                && event.getRosterSnapshot().contains("playerId")));
    }

    @Test
    void leagueGateRequiresEveryChampionshipTeamToBeLocked() {
        UUID championshipId = UUID.randomUUID();
        Team first = Team.builder().id(UUID.randomUUID()).build();
        Team second = Team.builder().id(UUID.randomUUID()).build();
        when(teams.findAllByChampionshipIdOrderByName(championshipId)).thenReturn(List.of(first, second));
        when(confirmations.findAllByChampionshipId(championshipId)).thenReturn(List.of(
                SquadConfirmation.builder().team(first).status(SquadConfirmationStatus.LOCKED).build(),
                SquadConfirmation.builder().team(second).status(SquadConfirmationStatus.CONFIRMED).build()));

        assertThatThrownBy(() -> service.assertLeagueReady(championshipId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Every team squad");

        when(confirmations.findAllByChampionshipId(championshipId)).thenReturn(List.of(
                SquadConfirmation.builder().team(first).status(SquadConfirmationStatus.LOCKED).build(),
                SquadConfirmation.builder().team(second).status(SquadConfirmationStatus.LOCKED).build()));
        assertThatCode(() -> service.assertLeagueReady(championshipId)).doesNotThrowAnyException();
    }

    private List<Player> roster(Championship championship, Team team, int male, int female) {
        List<Player> result = new ArrayList<>();
        for (int index = 0; index < male + female; index++) {
            result.add(Player.builder().id(UUID.randomUUID()).championship(championship).team(team)
                    .fullName("Player " + index).gender(index < male ? Gender.MALE : Gender.FEMALE)
                    .status(PlayerStatus.SOLD).build());
        }
        return result;
    }
}
