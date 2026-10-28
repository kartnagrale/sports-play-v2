"use client";
import { AuctionStateDto } from "@/lib/api";
import { rankTeams } from "@/lib/auction-live";
import { formatCr } from "@/lib/format";
import { PlayerAvatar } from "@/components/players/PlayerAvatar";
import { Card, StatusBadge } from "@/components/ui";
import { ReorderGroup, UpdatedValue } from "@/components/ui/Motion";
export function BiddingLeaderboard({ state,epoch }: { state: AuctionStateDto; epoch: number }) {
  const rows = rankTeams(state); const revision = rows.map(row => `${row.team.id}:${row.bid?.id || "none"}`).join("|");
  return <Card className="auction-leaderboard"><div className="flex items-start justify-between gap-3"><div><p className="label-cap">Current player</p><h2 className="section-title mt-2">Bidding leaderboard</h2></div><StatusBadge tone={state.status === "RUNNING" ? "live" : "neutral"}>{rows.filter(row => row.bid).length} bidding</StatusBadge></div><p className="mt-3 text-xs text-muted">Each team’s highest valid bid. No-bid teams follow below.</p>
    <ReorderGroup key={`${state.currentPlayer?.id || "waiting"}:${epoch}`} revision={revision} className="leaderboard-rows">{rows.map(({team,bid,rank}) => { const leading=!!bid && bid.id === state.highestBid?.id; return <div key={team.id} data-motion-id={team.id} data-testid={`bidder-${team.shortCode}`} data-rank={rank || "none"} className={`bidder-row ${leading ? "is-leading" : ""}`}><span className="bidder-rank">{rank ? String(rank).padStart(2,"0") : "—"}</span><PlayerAvatar name={team.name} imageUrl={team.logoUrl} shape="square" size="sm" alt="" /><div className="min-w-0 flex-1"><p className="break-words text-sm font-semibold">{team.name}</p><p className="mt-1 text-xs text-muted">{leading ? "Highest bidder" : bid ? "In the bidding" : "No bid for this player"}</p></div><div className="text-right text-sm font-semibold text-primary">{bid ? <UpdatedValue revision={bid.id}>{formatCr(Number(bid.amount))}</UpdatedValue> : <span className="text-muted">—</span>}</div></div>; })}</ReorderGroup>
    {!rows.length && <p className="py-8 text-center text-sm text-muted">Teams will appear when registered.</p>}
  </Card>;
}
