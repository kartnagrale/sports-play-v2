package com.neml.badminton.websocket;

import com.neml.badminton.dto.Dtos.AuctionStateDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;

@Component
public class AuctionBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public AuctionBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastState(java.util.UUID championshipId, java.util.UUID auctionId, AuctionStateDto state) {
        sendAfterCommit(topic(championshipId, auctionId), Map.of(
                "type", "STATE", "at", Instant.now().toString(), "data", state));
    }

    public void broadcastEvent(java.util.UUID championshipId, java.util.UUID auctionId, String eventType, Object data) {
        sendAfterCommit(topic(championshipId, auctionId), Map.of(
                "type", eventType, "at", Instant.now().toString(), "data", data));
    }

    private String topic(java.util.UUID championshipId, java.util.UUID auctionId) {
        return "/topic/championship/" + championshipId + "/auction/" + auctionId;
    }

    public void broadcastMatch(java.util.UUID championshipId, String eventType, Object data) {
        sendAfterCommit("/topic/championship/" + championshipId + "/matches", Map.of(
                "type", eventType, "at", Instant.now().toString(), "data", data));
    }

    private void sendAfterCommit(String destination, Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            messagingTemplate.convertAndSend(destination, payload); return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { messagingTemplate.convertAndSend(destination, payload); }
        });
    }
}
