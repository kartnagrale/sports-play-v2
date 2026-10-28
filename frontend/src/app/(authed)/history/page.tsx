"use client";
import { useState } from "react";
import { BidDto } from "@/lib/api";
import { useChampionship } from "@/lib/championship";
import { useTenantResource } from "@/lib/use-resource";
import { formatCr } from "@/lib/format";
import { Card, DataTable, EmptyState, ErrorState, PageHeader, SearchInput, Select, StatusBadge } from "@/components/ui";
import { PortalLoading } from "@/components/portal/Portal";
import { PlayerIdentity } from "@/components/players/PlayerAvatar";
import AuctionUnavailable from "@/components/auction/AuctionUnavailable";
export default function AuctionHistoryPage() {
  const active=useChampionship(s=>s.active); const history=useTenantResource<BidDto[]>(active?.defaultAuctionId ? `/auctions/${active.defaultAuctionId}/history` : null);
  const [search,setSearch]=useState("");const [status,setStatus]=useState("ALL");const [sort,setSort]=useState("recent");
  if(active&&!active.defaultAuctionId)return <AuctionUnavailable/>;
  if(history.loading)return <PortalLoading/>;
  if(history.error)return <ErrorState description={history.error} onRetry={history.refresh}/>;
  const visible=(history.data || []).filter(b=>`${b.playerName} ${b.teamName}`.toLowerCase().includes(search.toLowerCase())&&(status==="ALL"||(status==="ACTIVE"?b.active:!b.active))).sort((a,b)=>sort==="amount"?Number(b.amount)-Number(a.amount)||Date.parse(b.createdAt)-Date.parse(a.createdAt):Date.parse(b.createdAt)-Date.parse(a.createdAt));
  return <div className="portal-page"><PageHeader title="Auction history" eyebrow="Confirmed activity" description="The latest 50 recorded bids, including undone bids. Results stay linked to their player and team."/><div className="portal-toolbar"><SearchInput label="Search auction history" placeholder="Find a player or team…" value={search} onChange={e=>setSearch(e.target.value)}/><Select aria-label="Bid status" value={status} onChange={e=>setStatus(e.target.value)}><option value="ALL">All recorded bids</option><option value="ACTIVE">Valid bids</option><option value="UNDONE">Undone bids</option></Select><Select aria-label="Sort bids" value={sort} onChange={e=>setSort(e.target.value)}><option value="recent">Most recent</option><option value="amount">Highest amount</option></Select></div><Card className="p-0 overflow-hidden"><DataTable caption="Recorded auction bids"><thead><tr>{["Player","Team","Time","Status","Amount"].map(label=><th scope="col" key={label}>{label}</th>)}</tr></thead><tbody>{visible.map(b=><tr key={b.id}><td><PlayerIdentity name={b.playerName} imageUrl={b.playerPhotoUrl}/></td><td>{b.teamName}</td><td className="text-muted">{new Date(b.createdAt).toLocaleString()}</td><td><StatusBadge tone={b.active?"success":"neutral"}>{b.active?"Valid bid":"Undone"}</StatusBadge></td><td className={`font-mono ${b.active?"text-primary":"text-muted line-through"}`}>{formatCr(Number(b.amount))}</td></tr>)}</tbody></DataTable>{!visible.length&&<EmptyState title="No matching bidding activity" description="Confirmed bids appear here after a team enters the auction."/>}</Card></div>;
}
