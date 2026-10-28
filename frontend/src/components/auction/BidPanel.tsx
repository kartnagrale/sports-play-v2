"use client";
import { useState } from "react";
import { PlayerDto, TeamDto } from "@/lib/api";
import { formatCr } from "@/lib/format";
import { Button, Field, Input, Select } from "@/components/ui";
interface Props { minNext:number; increment:number; current:PlayerDto; teams:TeamDto[]; userTeamId?:string|null; isAdmin:boolean; disabled?:boolean; onBid:(teamId:string,amount:number)=>Promise<void> }
export default function BidPanel({ minNext,increment,current,teams,userTeamId,isAdmin,disabled,onBid }: Props) {
  const [teamDraft,setTeam] = useState(teams[0]?.id || "");
  const [draft,setDraft] = useState({ minimum:minNext,value:minNext });
  const [busy,setBusy] = useState(false);
  const amount = draft.minimum === minNext ? draft.value : minNext;
  const team = isAdmin ? (teams.some(t => t.id === teamDraft) ? teamDraft : teams[0]?.id || "") : userTeamId || "";
  async function submit(event:React.FormEvent) { event.preventDefault(); if (busy || disabled) return; setBusy(true); try { await onBid(team,amount); } finally { setBusy(false); } }
  return <form onSubmit={submit} className="bid-panel" data-testid="bid-panel" aria-label={`Bid for ${current.fullName}`}><div className="grid gap-4 sm:grid-cols-2">{isAdmin ? <Field label="Bidding team">{props => <Select {...props} data-testid="bid-team-select" value={team} onChange={e=>setTeam(e.target.value)} disabled={disabled || busy}>{teams.map(t=><option key={t.id} value={t.id}>{t.name}</option>)}</Select>}</Field> : <div><p className="field-label">Your team</p><p className="input">{teams.find(t=>t.id===team)?.name || "No team assigned"}</p></div>}<Field label="Bid amount (₹)" hint={`Minimum ${formatCr(minNext)} · Increment ${formatCr(increment)}`}>{props=><Input {...props} type="number" data-testid="bid-amount" min={minNext} step="any" required value={amount} onChange={e=>setDraft({minimum:minNext,value:Number(e.target.value)})} disabled={disabled || busy}/>}</Field></div><div className="mt-4 flex flex-wrap gap-3"><Button variant="ghost" onClick={()=>setDraft({minimum:minNext,value:minNext})} disabled={disabled || busy} data-testid="bid-quick-min">Use minimum · {formatCr(minNext)}</Button><Button type="submit" loading={busy} disabled={disabled || !team || !Number.isFinite(amount) || amount<minNext} className="flex-1" data-testid="place-bid-btn">{busy ? "Submitting bid…" : "Place bid"}</Button></div></form>;
}
