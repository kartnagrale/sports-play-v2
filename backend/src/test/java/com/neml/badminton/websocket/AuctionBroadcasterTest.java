package com.neml.badminton.websocket;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.*;
import java.util.*;
import static org.mockito.Mockito.*;
class AuctionBroadcasterTest {
    @Test void matchEventsArePublishedOnlyAfterCommit(){
        var template=mock(SimpMessagingTemplate.class);var broadcaster=new AuctionBroadcaster(template);var cid=UUID.randomUUID();
        TransactionSynchronizationManager.initSynchronization();TransactionSynchronizationManager.setActualTransactionActive(true);
        try{
            broadcaster.broadcastMatch(cid,"MATCH_RESULT",Map.of("revision",2));
            verifyNoInteractions(template);
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(template).convertAndSend(eq("/topic/championship/"+cid+"/matches"),any(Object.class));
        }finally{TransactionSynchronizationManager.clearSynchronization();TransactionSynchronizationManager.setActualTransactionActive(false);}
    }
    @Test void rolledBackEventsAreNeverPublished(){
        var template=mock(SimpMessagingTemplate.class);var broadcaster=new AuctionBroadcaster(template);
        TransactionSynchronizationManager.initSynchronization();TransactionSynchronizationManager.setActualTransactionActive(true);
        try{
            broadcaster.broadcastMatch(UUID.randomUUID(),"MATCH_RESULT",Map.of("revision",2));
            TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verifyNoInteractions(template);
        }finally{TransactionSynchronizationManager.clearSynchronization();TransactionSynchronizationManager.setActualTransactionActive(false);}
    }
    @Test void auctionSaleWaitsForCommitAndRollbackEmitsNothing(){
        var template=mock(SimpMessagingTemplate.class);var broadcaster=new AuctionBroadcaster(template);
        TransactionSynchronizationManager.initSynchronization();TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            broadcaster.broadcastEvent(UUID.randomUUID(),UUID.randomUUID(),"PLAYER_SOLD",Map.of("saleId",UUID.randomUUID(),"revision",12));
            verifyNoInteractions(template);
            TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verifyNoInteractions(template);
        } finally {TransactionSynchronizationManager.clearSynchronization();TransactionSynchronizationManager.setActualTransactionActive(false);}
        TransactionSynchronizationManager.initSynchronization();TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            broadcaster.broadcastEvent(UUID.randomUUID(),UUID.randomUUID(),"PLAYER_SOLD",Map.of("saleId",UUID.randomUUID(),"revision",13));
            verifyNoInteractions(template);
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(template).convertAndSend(contains("/auction/"),any(Object.class));
        } finally {TransactionSynchronizationManager.clearSynchronization();TransactionSynchronizationManager.setActualTransactionActive(false);}
    }
}
