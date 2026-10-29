package com.neml.badminton.service;
import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class AuctionPolicyGuardTest {
    ChampionshipRepository championships=mock(ChampionshipRepository.class);
    AuctionRepository auctions=mock(AuctionRepository.class);
    TeamRepository teams=mock(TeamRepository.class);
    BidRepository bids=mock(BidRepository.class);
    ConfigurationSectionStateRepository sections=mock(ConfigurationSectionStateRepository.class);
    AuctionPolicyGuard guard=new AuctionPolicyGuard(championships,auctions,teams,bids,sections);
    UUID cid=UUID.randomUUID();
    @Test void evenUndoneHistoricalBidsPreventReconfigurationAfterReset(){
        when(championships.findByIdForUpdate(cid)).thenReturn(Optional.of(new Championship()));
        when(bids.existsByChampionshipId(cid)).thenReturn(true);
        assertThatThrownBy(()->guard.lockEditable(cid)).hasMessageContaining("locked after the first auction action");
    }
    @Test void untouchedChampionshipIsEditableUnderSharedLock(){
        Championship c=new Championship();when(championships.findByIdForUpdate(cid)).thenReturn(Optional.of(c));
        assertThat(guard.lockEditable(cid)).isSameAs(c);verify(championships).findByIdForUpdate(cid);
    }
    @Test void persistedAuctionSectionLockSurvivesStatusResetAndMissingBidHistory(){
        when(championships.findByIdForUpdate(cid)).thenReturn(Optional.of(new Championship()));
        when(sections.findByChampionshipIdAndSectionCode(cid,"AUCTION")).thenReturn(Optional.of(
                ConfigurationSectionState.builder().status(ConfigurationStatus.LOCKED).build()));
        assertThatThrownBy(()->guard.lockEditable(cid)).hasMessageContaining("locked after the first auction action");
    }
}
