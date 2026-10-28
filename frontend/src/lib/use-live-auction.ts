"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { api, AuctionStateDto, PlayerDto } from "./api";
import { AuctionReconciler, ConfirmedSale } from "./auction-live";
import { createAuctionSocket } from "./ws";
export function useLiveAuction(championshipId?: string, auctionId?: string | null) {
  const [state,setState] = useState<AuctionStateDto | null>(null);
  const [players,setPlayers] = useState<PlayerDto[]>([]);
  const [connection,setConnection] = useState("connecting");
  const [error,setError] = useState("");
  const [sale,setSale] = useState<ConfirmedSale | null>(null);
  const [outcome,setOutcome] = useState<{text:string;revision:number} | null>(null);
  const [epoch,setEpoch] = useState(0);
  const refreshRef = useRef<() => Promise<void>>(async () => {});
  const commitRef = useRef<(state: AuctionStateDto) => void>(() => {});
  useEffect(() => {
    if (!championshipId || !auctionId) return;
    const base = `/championships/${championshipId}/auctions/${auctionId}`;
    const model = new AuctionReconciler();
    const controller = new AbortController(); let connected = false, generation = 0, playerTicket = 0;
    let buffered: AuctionStateDto | null = null;
    let playerRequest: Promise<void> | null = null;
    const loadPlayers = (): Promise<void> => playerRequest ||= (async () => {
      const ticket = ++playerTicket;
      try { const { data } = await api.get<PlayerDto[]>(`/championships/${championshipId}/players`,{ signal: controller.signal }); if (!controller.signal.aborted && ticket === playerTicket) setPlayers(data); } catch { /* Auction state remains usable; explicit refresh retries this list. */ } finally { playerRequest = null; }
    })();
    const apply = (next: AuctionStateDto) => {
      if (controller.signal.aborted) return;
      const prior = model.snapshot;
      if (!model.accept(next)) return;
      setState(next); setError("");
      if (prior?.currentPlayer?.id !== next.currentPlayer?.id) { void loadPlayers(); }
      setOutcome(value => value && next.revision > value.revision ? null : value);
      setSale(value => value && next.revision > value.revision ? null : value);
    };
    const reconcile = async (baseline = false,refreshPlayers = false) => {
      const ticket = ++generation;
      if (baseline) { model.disconnect(); setConnection("syncing"); }
      try {
        const { data } = await api.get<AuctionStateDto>(base+"/state",{ signal: controller.signal });
        if (controller.signal.aborted || ticket !== generation) return;
        const newest = buffered && buffered.revision > data.revision ? buffered : data; buffered = null;
        if (baseline && connected) { model.synchronize(newest); setState(newest); setConnection("live"); setEpoch(value => value+1); setError(""); }
        else apply(newest);
        if(baseline || refreshPlayers) await loadPlayers();
      } catch (failure: any) { if (!controller.signal.aborted && ticket === generation) { setError(failure?.response?.data?.message || "Could not refresh auction state."); if (baseline) setConnection("interrupted"); } }
    };
    refreshRef.current = () => reconcile(connected && !model.ready,true);
    commitRef.current = next => { if (model.ready) apply(next); else if (!buffered || next.revision > buffered.revision) buffered = next; };
    void reconcile();
    const client = createAuctionSocket(event => {
      if (controller.signal.aborted) return;
      if (event.type === "STATE") {
        if (!model.ready) { if (!buffered || event.data.revision > buffered.revision) buffered = event.data; }
        else apply(event.data);
      } else if (event.type === "PLAYER_PHOTO_UPDATED" && connected && model.ready) void reconcile(false,true);
      else if (event.type === "PLAYER_SOLD" && connected && model.sale(event.data)) setSale(event.data);
      else if (event.type === "COIN_TOSS" && connected && model.ready) setOutcome({text:`Coin toss winner: ${event.data.winnerTeamName}`,revision:model.snapshot!.revision});
      else if (event.type === "PLAYER_UNSOLD" && connected && model.matchesResult(event.data.playerId,event.data.revision)) setOutcome({text:`${event.data.player} went unsold. No purchase was made.`,revision:event.data.revision});
    }, online => {
      if (controller.signal.aborted) return;
      connected = online; setSale(null); setOutcome(null);
      if (online) void reconcile(true);
      else { generation++; buffered = null; model.disconnect(); setConnection("interrupted"); }
    });
    const interval = window.setInterval(() => { if (connected && model.ready) void reconcile(); }, 15000);
    return () => { controller.abort(); window.clearInterval(interval); void client.deactivate(); refreshRef.current = async () => {}; commitRef.current = () => {}; };
  },[championshipId,auctionId]);
  const refresh = useCallback(() => refreshRef.current(),[]);
  const commit = useCallback((next: AuctionStateDto) => commitRef.current(next),[]);
  const dismissSale=useCallback(()=>setSale(null),[]);
  return { state,players,connection,error,sale,outcome:outcome?.text || "",epoch,refresh,commit,dismissSale };
}
