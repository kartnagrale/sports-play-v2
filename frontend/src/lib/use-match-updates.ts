"use client";
import { useEffect, useRef, useState } from "react";
import { useChampionship } from "./championship";
import { createMatchSocket } from "./ws";
import { createRefreshQueue } from "./live-refresh";

/** One tenant-bound subscription, reconnect resync and bounded fallback; callbacks never capture the old tenant. */
export function useMatchUpdates(refresh: () => Promise<unknown> | unknown) {
  const cid = useChampionship(s => s.active?.id);
  const latest = useRef(refresh);useEffect(() => { latest.current = refresh; }, [refresh]);
  const [connection, setConnection] = useState("connecting");
  useEffect(() => {
    if (!cid) return;
    let disposed = false;
    const queue = createRefreshQueue(() => latest.current());
    const client = createMatchSocket(() => queue.request(), connected => {
      if (disposed) return;
      setConnection(connected ? "live" : "interrupted");
      if (connected) queue.request();
    }, cid);
    const interval = setInterval(() => queue.request(), 30000);
    return () => { disposed = true; queue.dispose(); clearInterval(interval); void client.deactivate(); };
  }, [cid]);
  return connection;
}
