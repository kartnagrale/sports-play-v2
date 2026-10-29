package com.neml.badminton.service;

import com.neml.badminton.entity.*;
import com.neml.badminton.repository.*;
import com.neml.badminton.websocket.AuctionBroadcaster;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuctionPresentationTest {
    final AuctionRepository auctions=mock(AuctionRepository.class);
    final AuctionStateRepository states=mock(AuctionStateRepository.class);
    final PlayerRepository players=mock(PlayerRepository.class);
    final TeamRepository teams=mock(TeamRepository.class);
    final BidRepository bids=mock(BidRepository.class);
    final TournamentSettingsRepository settings=mock(TournamentSettingsRepository.class);
    final UUID cid=UUID.randomUUID(), aid=UUID.randomUUID();
    final Auction auction=mock(Auction.class);
    final AuctionState state=mock(AuctionState.class);
    final AuctionRuleEvaluator evaluator=mock(AuctionRuleEvaluator.class);
    final TenantAuctionService service=new TenantAuctionService(auctions,states,players,teams,bids,settings,
            mock(AuctionBroadcaster.class),mock(TransactionTemplate.class),mock(SquadConfirmationService.class),
            mock(ChampionshipRepository.class),mock(ConfigurationSectionService.class),mock(jakarta.persistence.EntityManager.class),evaluator);
    void prepare() {
        when(evaluator.rules(cid)).thenReturn(new AuctionBidRules.Rules(11,11,0,3,BigDecimal.valueOf(5000),BigDecimal.valueOf(15000),4));
        when(auctions.findByIdAndChampionshipId(aid,cid)).thenReturn(Optional.of(auction));
        when(states.findByAuctionIdAndChampionshipId(aid,cid)).thenReturn(Optional.of(state));
        when(settings.findByChampionshipId(cid)).thenReturn(Optional.of(mock(TournamentSettings.class)));
        when(auction.getId()).thenReturn(aid);
        when(state.getVersion()).thenReturn(17L);
        when(state.getStatus()).thenReturn(AuctionStatus.RUNNING);
    }
    @Test void snapshotExposesPersistedRevisionAndCompleteCurrentPlayerTeamBids() {
        prepare();
        Player player=mock(Player.class); Team team=mock(Team.class); Bid bid=mock(Bid.class);
        UUID pid=UUID.randomUUID(), tid=UUID.randomUUID(), bidId=UUID.randomUUID();
        when(state.getCurrentPlayer()).thenReturn(player);when(player.getId()).thenReturn(pid);
        when(bid.getId()).thenReturn(bidId);when(bid.getPlayer()).thenReturn(player);when(bid.getTeam()).thenReturn(team);
        when(team.getId()).thenReturn(tid);when(bid.getAmount()).thenReturn(BigDecimal.valueOf(50000));when(bid.getActive()).thenReturn(true);
        // History is deliberately empty: leaderboard must use the independent complete query.
        when(bids.findCurrentTeamBids(aid,cid,player)).thenReturn(List.of(bid));
        var result=service.state(cid,aid);
        assertEquals(17L,result.revision());assertTrue(result.bidHistory().isEmpty());
        assertEquals(bidId,result.teamBids().get(0).id());assertEquals(tid,result.teamBids().get(0).teamId());
        verify(states).flush();verify(bids).findCurrentTeamBids(aid,cid,player);
    }
    @Test void playerlessSnapshotDoesNotLeakPreviousPlayerRankings() {
        prepare();var result=service.state(cid,aid);
        assertTrue(result.teamBids().isEmpty());assertNull(result.highestBid());
        verify(bids,never()).findCurrentTeamBids(any(),any(),any());
        verify(players).countByAuctionIdAndChampionshipIdAndStatus(aid,cid,PlayerStatus.AVAILABLE);
        verify(players,never()).findAllByAuctionIdAndChampionshipIdAndStatusOrderByAuctionOrderAsc(any(),any(),any());
    }
    @Test void foreignAuctionCannotReturnRankingData() {
        when(auctions.findByIdAndChampionshipId(aid,cid)).thenReturn(Optional.empty());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.state(cid,aid));
        verifyNoInteractions(bids);
    }
}
