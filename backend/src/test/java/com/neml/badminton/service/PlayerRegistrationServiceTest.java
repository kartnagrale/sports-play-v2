package com.neml.badminton.service;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class PlayerRegistrationServiceTest {
    @Test void manualAndBulkDefaultsReserveExplicitOrdersAndRejectReferences(){
        var champs=mock(ChampionshipRepository.class);var auctions=mock(AuctionRepository.class);var players=mock(PlayerRepository.class);var settings=mock(TournamentSettingsRepository.class);var squads=mock(SquadConfirmationService.class);
        var cid=UUID.randomUUID();var champ=Championship.builder().id(cid).lifecyclePhase(ChampionshipPhase.CONFIGURATION).build();var auction=Auction.builder().id(UUID.randomUUID()).build();
        when(champs.findByIdForUpdate(cid)).thenReturn(Optional.of(champ));when(auctions.findAllByChampionshipIdOrderByCreatedAtDesc(cid)).thenReturn(List.of(auction));
        when(settings.findByChampionshipId(cid)).thenReturn(Optional.of(TournamentSettings.builder().playerBasePrice(new BigDecimal("50000")).build()));when(players.findAllByChampionshipIdOrderByAuctionOrderAsc(cid)).thenReturn(List.of());when(players.save(any())).thenAnswer(a->a.getArgument(0));
        var service=new PlayerRegistrationService(champs,auctions,players,settings,squads,mock(AuctionConfigurationRepository.class));
        var result=service.register(cid,List.of(new PlayerRegistrationService.Registration(" One ",Gender.MALE,null,null,null,"one"),new PlayerRegistrationService.Registration("Two",Gender.FEMALE,null,null,1,"two")));
        assertThat(result.get(0).getAuctionOrder()).isEqualTo(2);assertThat(result.get(1).getAuctionOrder()).isEqualTo(1);assertThat(result.get(0).getBasePrice()).isEqualByComparingTo("50000");assertThat(result.get(0).getFullName()).isEqualTo("One");assertThat(result.get(0).getStatus()).isEqualTo(PlayerStatus.AVAILABLE);verify(squads).assertRosterMutable(cid);
        assertThatThrownBy(()->service.register(cid,List.of(new PlayerRegistrationService.Registration("One",Gender.MALE,null,null,null,"one"),new PlayerRegistrationService.Registration("Two",Gender.MALE,null,null,null,"ONE")))).hasMessageContaining("Reference");
        champ.setLifecyclePhase(ChampionshipPhase.AUCTION);assertThatThrownBy(()->service.register(cid,List.of())).hasMessageContaining("closed");
    }
}
