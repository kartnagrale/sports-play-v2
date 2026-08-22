"use client";

import { Client, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { WS_BASE } from "./api";
import { activeTenant, VIEWER_TOKEN_KEY } from "./championship";

export type AuctionEvent = {
  type: string;
  at: string;
  data: any;
};

export function createAuctionSocket(onEvent: (evt: AuctionEvent) => void): Client {
  const { championshipId, auctionId } = activeTenant();
  if (!championshipId || !auctionId) throw new Error("Select a championship before connecting to its auction");
  return createSocket(`/topic/championship/${championshipId}/auction/${auctionId}`, onEvent);
}

export function createMatchSocket(onEvent: (evt: AuctionEvent) => void): Client {
  const { championshipId } = activeTenant();
  if (!championshipId) throw new Error("Select a championship before connecting to matches");
  return createSocket(`/topic/championship/${championshipId}/matches`, onEvent);
}

function createSocket(topic: string, onEvent: (evt: AuctionEvent) => void): Client {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${WS_BASE}/ws`) as any,
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {},
    connectHeaders: typeof window !== "undefined" && localStorage.getItem(VIEWER_TOKEN_KEY)
      ? { Authorization: `Bearer ${localStorage.getItem(VIEWER_TOKEN_KEY)}` } : {},
  });
  client.onConnect = () => {
    client.subscribe(topic, (msg: IMessage) => {
      try {
        const parsed = JSON.parse(msg.body);
        onEvent(parsed);
      } catch (e) {
        console.warn("bad ws msg", e);
      }
    });
  };
  client.activate();
  return client;
}
