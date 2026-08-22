package com.neml.badminton.websocket;

import com.neml.badminton.dto.Dtos.AuctionStateDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class AuctionBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public AuctionBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastState(AuctionStateDto state) {
        messagingTemplate.convertAndSend("/topic/auction", Map.of(
                "type", "STATE",
                "at", Instant.now().toString(),
                "data", state
        ));
    }

    public void broadcastState(java.util.UUID championshipId, java.util.UUID auctionId, AuctionStateDto state) {
        messagingTemplate.convertAndSend(topic(championshipId, auctionId), Map.of(
                "type", "STATE", "at", Instant.now().toString(), "data", state));
    }

    public void broadcastEvent(java.util.UUID championshipId, java.util.UUID auctionId, String eventType, Object data) {
        messagingTemplate.convertAndSend(topic(championshipId, auctionId), Map.of(
                "type", eventType, "at", Instant.now().toString(), "data", data));
    }

    private String topic(java.util.UUID championshipId, java.util.UUID auctionId) {
        return "/topic/championship/" + championshipId + "/auction/" + auctionId;
    }

    public void broadcastEvent(String eventType, Object data) {
        messagingTemplate.convertAndSend("/topic/auction", Map.of(
                "type", eventType,
                "at", Instant.now().toString(),
                "data", data
        ));
    }

    public void broadcastMatch(String eventType, Object data) {
        messagingTemplate.convertAndSend("/topic/matches", Map.of(
                "type", eventType,
                "at", Instant.now().toString(),
                "data", data
        ));
    }

    public void broadcastMatch(java.util.UUID championshipId, String eventType, Object data) {
        messagingTemplate.convertAndSend("/topic/championship/" + championshipId + "/matches", Map.of(
                "type", eventType, "at", Instant.now().toString(), "data", data));
    }
}
