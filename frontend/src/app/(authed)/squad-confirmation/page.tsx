"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import {
  AlertTriangle, ArrowRight, Check, CheckCircle2, ClipboardCheck, Clock3,
  Lock, RefreshCw, ShieldCheck, Unlock, UserCheck, UsersRound,
} from "lucide-react";
import { api, PlayerDto, SquadEventDto, SquadSummaryDto, SquadTeamDto } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useChampionship } from "@/lib/championship";
import { useNavigation } from "@/lib/navigation";

export default function SquadConfirmationPage() {
  const { active } = useChampionship();
  const { user } = useAuth();
  const { role } = useNavigation();
  const [summary, setSummary] = useState<SquadSummaryDto | null>(null);
  const [history, setHistory] = useState<SquadEventDto[]>([]);
  const [players, setPlayers] = useState<PlayerDto[]>([]);
  const [selectedTeamId, setSelectedTeamId] = useState<string | null>(null);
  const [notes, setNotes] = useState("");
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState<string | null>(null);

  const access = user?.championships.find((item) => item.championshipId === active?.id);
  const isAdmin = role === "CHAMPIONSHIP_ADMIN";
  const captainTeamId = role === "TEAM_CAPTAIN" ? access?.teamId : null;

  const load = useCallback(async () => {
    if (!active) return;
    setLoading(true);
    try {
      const [summaryResponse, historyResponse, playerResponse] = await Promise.all([
        api.get<SquadSummaryDto>(`/championships/${active.id}/squad-confirmations`),
        api.get<SquadEventDto[]>(`/championships/${active.id}/squad-confirmations/history`),
        api.get<PlayerDto[]>(`/championships/${active.id}/players`),
      ]);
      setSummary(summaryResponse.data);
      setHistory(historyResponse.data);
      setPlayers(playerResponse.data);
      setSelectedTeamId((current) => captainTeamId || current || summaryResponse.data.teams[0]?.teamId || null);
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Unable to load squad confirmation");
    } finally {
      setLoading(false);
    }
  }, [active, captainTeamId]);

  useEffect(() => { load(); }, [load]);

  const selected = summary?.teams.find((team) => team.teamId === selectedTeamId) || null;
  const roster = useMemo(() => players.filter((player) => player.teamId === selectedTeamId), [players, selectedTeamId]);
  const progress = summary?.totalTeams ? Math.round((summary.lockedTeams / summary.totalTeams) * 100) : 0;

  async function action(kind: "confirm" | "lock" | "reopen" | "lock-all", teamId?: string) {
    if (!active) return;
    const key = `${kind}-${teamId || "all"}`;
    setWorking(key);
    try {
      const path = kind === "lock-all"
        ? `/championships/${active.id}/squad-confirmations/lock-all`
        : `/championships/${active.id}/squad-confirmations/teams/${teamId}/${kind}`;
      const { data } = await api.post<SquadSummaryDto>(path, { notes: notes.trim() || null });
      setSummary(data);
      setNotes("");
      const { data: activity } = await api.get<SquadEventDto[]>(`/championships/${active.id}/squad-confirmations/history`);
      setHistory(activity);
      toast.success(kind === "confirm" ? "Squad submitted for approval" : kind === "reopen" ? "Squad reopened" : "Squad locked successfully");
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Squad action failed");
    } finally {
      setWorking(null);
    }
  }

  if (loading || !summary) return <LoadingState />;

  return (
    <div className="min-h-screen p-5 md:p-10">
      <header className="mb-8 flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <div className="label-cap text-primary">Phase 2 · Competition control</div>
          <h1 className="h-heading mt-2 text-4xl font-bold md:text-5xl">Squad Confirmation</h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-white/50">
            Captains verify their auction roster. The championship administrator locks every approved squad before league fixtures can begin.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <PhaseChip status={summary.phaseStatus}/>
          <button className="btn btn-ghost" onClick={load} disabled={loading}><RefreshCw size={15}/>Refresh</button>
        </div>
      </header>

      <section className={`relative mb-8 overflow-hidden rounded-3xl border p-6 md:p-8 ${summary.allSquadsLocked ? "border-primary/30 bg-primary/[.06]" : "border-white/10 bg-white/[.025]"}`}>
        <div className="absolute -right-16 -top-20 h-56 w-56 rounded-full bg-primary/10 blur-3xl"/>
        <div className="relative grid gap-7 lg:grid-cols-[1fr_auto] lg:items-center">
          <div>
            <div className="flex items-center gap-3">
              <div className="grid h-12 w-12 place-items-center rounded-2xl border border-primary/25 bg-primary/10 text-primary"><ClipboardCheck size={23}/></div>
              <div><div className="label-cap text-[10px]">Championship gate</div><h2 className="h-heading mt-1 text-2xl">{summary.allSquadsLocked ? "League stage unlocked" : "Awaiting locked squads"}</h2></div>
            </div>
            <div className="mt-6 h-2 overflow-hidden rounded-full bg-white/10"><div className="h-full rounded-full bg-primary transition-all duration-700" style={{width:`${progress}%`}}/></div>
            <div className="mt-3 flex flex-wrap justify-between gap-2 text-xs text-white/45"><span>{summary.lockedTeams} of {summary.totalTeams} squads locked</span><span>{progress}% phase completion</span></div>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Metric label="Ready" value={`${summary.readyTeams}/${summary.totalTeams}`} tone="cyan"/>
            <Metric label="Confirmed" value={summary.confirmedTeams.toString()} tone="amber"/>
            <Metric label="Locked" value={summary.lockedTeams.toString()} tone="green"/>
          </div>
        </div>
        {summary.allSquadsLocked && <div className="relative mt-6 flex flex-col gap-3 rounded-2xl border border-primary/20 bg-black/15 p-4 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-center gap-3 text-sm"><CheckCircle2 className="text-primary" size={20}/><span>All squads passed validation. Phase 3 league scheduling is now enabled.</span></div><Link href="/matches" className="btn btn-primary justify-center">Open league matches<ArrowRight size={15}/></Link></div>}
      </section>

      {!summary.auctionCompleted && <div className="mb-6 flex items-start gap-3 rounded-2xl border border-warning/30 bg-warning/10 p-4 text-sm text-warning"><AlertTriangle className="mt-0.5 shrink-0" size={18}/><span>The auction must be completed before captains can confirm their squads.</span></div>}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.55fr)_minmax(330px,.75fr)]">
        <div className="space-y-6">
          <section className="grid gap-4 md:grid-cols-2">
            {summary.teams.map((team) => <TeamCard key={team.teamId} team={team} selected={team.teamId===selectedTeamId} disabled={Boolean(captainTeamId && captainTeamId!==team.teamId)} onClick={()=>setSelectedTeamId(team.teamId)}/>) }
          </section>

          {selected && <section className="card-elev overflow-hidden rounded-3xl">
            <div className="flex flex-col gap-5 border-b border-white/10 p-6 md:flex-row md:items-center md:justify-between">
              <div className="flex items-center gap-4"><TeamMark team={selected}/><div><div className="label-cap">Roster review</div><h2 className="h-heading mt-1 text-2xl">{selected.teamName}</h2></div></div>
              <div className="flex flex-wrap gap-2"><RuleChip valid={selected.playerCount===selected.requiredPlayers} label={`${selected.playerCount}/${selected.requiredPlayers} players`}/><RuleChip valid={selected.maleCount>=selected.requiredMale} label={`${selected.maleCount} male`}/><RuleChip valid={selected.femaleCount>=selected.requiredFemale} label={`${selected.femaleCount} female`}/></div>
            </div>
            {selected.validationIssues.length>0 && <div className="mx-6 mt-5 rounded-xl border border-danger/30 bg-danger/10 p-4 text-sm text-danger">{selected.validationIssues.map(issue=><div key={issue}>• {issue}</div>)}</div>}
            <div className="overflow-x-auto px-6 pb-6">
              <table className="mt-3 w-full min-w-[620px] text-sm">
                <thead><tr className="border-b border-white/10 text-left label-cap text-[9px]"><th className="py-3">#</th><th>Player</th><th>Gender</th><th>Skill</th><th>Status</th></tr></thead>
                <tbody>{roster.map((player,index)=><tr key={player.id} className="border-b border-white/5"><td className="py-3 text-white/30">{String(index+1).padStart(2,"0")}</td><td className="font-medium">{player.fullName}</td><td><span className="chip">{player.gender}</span></td><td className="text-white/55">{player.skillLevel||"—"}</td><td><span className="inline-flex items-center gap-1.5 text-xs text-primary"><Check size={12}/>{player.status}</span></td></tr>)}</tbody>
              </table>
            </div>
          </section>}
        </div>

        <aside className="space-y-6">
          {selected && <section className="card-elev rounded-3xl p-6">
            <div className="flex items-center gap-3"><ShieldCheck className="text-primary" size={20}/><div><div className="label-cap">Approval desk</div><h3 className="h-heading mt-1 text-xl">{isAdmin ? "Administrator controls" : "Captain declaration"}</h3></div></div>
            <div className="my-5 border-t border-white/10"/>
            <StatusTimeline team={selected}/>
            <label className="mt-5 block"><span className="label-cap mb-2 block text-[9px]">Optional note</span><textarea className="input min-h-24 resize-none" maxLength={500} value={notes} onChange={event=>setNotes(event.target.value)} placeholder={isAdmin?"Add an approval or reopening note":"I have reviewed this squad…"}/></label>
            {role==="TEAM_CAPTAIN" && selected.teamId===captainTeamId && selected.status==="DRAFT" && <button data-testid="confirm-squad" className="btn btn-primary mt-4 w-full justify-center py-3" disabled={!selected.rosterValid||!summary.auctionCompleted||Boolean(working)} onClick={()=>action("confirm",selected.teamId)}><UserCheck size={16}/>{working?"Submitting…":"Confirm this squad"}</button>}
            {isAdmin && selected.status==="CONFIRMED" && <button data-testid="lock-squad" className="btn btn-primary mt-4 w-full justify-center py-3" disabled={Boolean(working)} onClick={()=>action("lock",selected.teamId)}><Lock size={16}/>{working?"Locking…":"Approve & lock squad"}</button>}
            {isAdmin && selected.status!=="DRAFT" && <button data-testid="reopen-squad" className="btn btn-ghost mt-3 w-full justify-center" disabled={Boolean(working)} onClick={()=>action("reopen",selected.teamId)}><Unlock size={15}/>Reopen squad</button>}
            {selected.status==="LOCKED" && <div className="mt-4 rounded-xl border border-primary/20 bg-primary/5 p-4 text-xs leading-5 text-white/55"><Lock className="mb-2 text-primary" size={17}/>This roster is immutable. League match creation will unlock once every team reaches this state.</div>}
          </section>}

          {isAdmin && !summary.allSquadsLocked && <section className="rounded-3xl border border-primary/20 bg-primary/[.045] p-6"><div className="label-cap text-primary">Final admin action</div><h3 className="h-heading mt-2 text-xl">Lock all confirmed squads</h3><p className="mt-2 text-xs leading-5 text-white/45">Available only after every captain has confirmed a valid squad.</p><button data-testid="lock-all-squads" className="btn btn-primary mt-5 w-full justify-center" disabled={summary.confirmedTeams+summary.lockedTeams!==summary.totalTeams||Boolean(working)} onClick={()=>action("lock-all")}><Lock size={15}/>Lock all & complete Phase 2</button></section>}

          <section className="card-elev rounded-3xl p-6"><div className="flex items-center gap-2"><Clock3 className="text-primary" size={18}/><h3 className="h-heading text-lg">Recent activity</h3></div><div className="mt-5 space-y-4">{history.length===0?<p className="text-xs text-white/35">No confirmation activity yet.</p>:history.slice(0,8).map(event=><div key={event.id} className="relative border-l border-white/10 pl-4"><span className="absolute -left-1 top-1 h-2 w-2 rounded-full bg-primary"/><div className="text-xs font-medium">{event.teamName} · {event.action}</div><div className="mt-1 text-[10px] text-white/35">{event.actor.fullName} · {new Date(event.occurredAt).toLocaleString("en-IN")}</div>{event.notes&&<div className="mt-1 text-[10px] text-white/45">{event.notes}</div>}</div>)}</div></section>
        </aside>
      </div>
    </div>
  );
}

function TeamCard({team,selected,disabled,onClick}:{team:SquadTeamDto;selected:boolean;disabled:boolean;onClick:()=>void}) {
  return <button onClick={onClick} disabled={disabled} className={`rounded-2xl border p-5 text-left transition-all ${selected?"border-primary/50 bg-primary/[.07] shadow-[0_14px_50px_rgba(124,255,107,.06)]":"border-white/10 bg-white/[.025] hover:-translate-y-0.5 hover:border-white/20"} ${disabled?"cursor-default opacity-50":""}`}><div className="flex items-center gap-3"><TeamMark team={team}/><div className="min-w-0 flex-1"><div className="truncate h-heading text-lg">{team.teamName}</div><div className="mt-1 text-xs text-white/40">{team.playerCount} players · {team.maleCount}M / {team.femaleCount}F</div></div><StatusIcon status={team.status}/></div><div className="mt-4 flex items-center justify-between"><StatusLabel status={team.status}/><span className={`text-[10px] ${team.rosterValid?"text-primary":"text-danger"}`}>{team.rosterValid?"Roster valid":"Needs attention"}</span></div></button>;
}
function TeamMark({team}:{team:SquadTeamDto}) { return <div className="grid h-11 w-11 shrink-0 place-items-center rounded-xl border h-heading text-xs font-bold" style={{color:team.primaryColor,borderColor:`${team.primaryColor}55`,background:`${team.primaryColor}16`}}>{team.shortCode}</div>; }
function StatusIcon({status}:{status:SquadTeamDto["status"]}) { return status==="LOCKED"?<Lock className="text-primary" size={18}/>:status==="CONFIRMED"?<UserCheck className="text-warning" size={19}/>:<UsersRound className="text-white/30" size={19}/>; }
function StatusLabel({status}:{status:SquadTeamDto["status"]}) { const style=status==="LOCKED"?"chip chip-primary":status==="CONFIRMED"?"chip text-warning border-warning/30 bg-warning/10":"chip"; return <span className={style}>{status}</span>; }
function RuleChip({valid,label}:{valid:boolean;label:string}) { return <span className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-[10px] ${valid?"border-primary/20 bg-primary/5 text-primary":"border-danger/30 bg-danger/10 text-danger"}`}>{valid?<Check size={11}/>:<AlertTriangle size={11}/>} {label}</span>; }
function PhaseChip({status}:{status:SquadSummaryDto["phaseStatus"]}) { const active=status==="COMPLETED"?"chip chip-primary":status==="IN_PROGRESS"?"chip text-warning border-warning/30 bg-warning/10":"chip"; return <span className={active}>{status.replaceAll("_"," ")}</span>; }
function Metric({label,value,tone}:{label:string;value:string;tone:"green"|"cyan"|"amber"}) { const color=tone==="green"?"text-primary":tone==="cyan"?"text-secondary":"text-warning"; return <div className="min-w-24 rounded-2xl border border-white/10 bg-black/15 px-4 py-3 text-center"><div className={`stat-num text-2xl ${color}`}>{value}</div><div className="label-cap mt-1 text-[8px]">{label}</div></div>; }
function StatusTimeline({team}:{team:SquadTeamDto}) { const stage=team.status==="LOCKED"?3:team.status==="CONFIRMED"?2:1; return <div className="space-y-4">{[[1,"Roster ready","Auction allocation validated"],[2,"Captain confirmed",team.confirmedBy?`By ${team.confirmedBy.fullName}`:"Awaiting captain"],[3,"Admin locked",team.lockedBy?`By ${team.lockedBy.fullName}`:"Awaiting administrator"]].map(([number,label,detail])=>{const done=stage>=Number(number);return <div key={String(number)} className="flex gap-3"><div className={`grid h-7 w-7 shrink-0 place-items-center rounded-full border text-[10px] ${done?"border-primary/40 bg-primary/10 text-primary":"border-white/10 text-white/25"}`}>{done?<Check size={13}/>:number}</div><div><div className={`text-sm ${done?"text-white":"text-white/35"}`}>{label}</div><div className="mt-0.5 text-[10px] text-white/30">{detail}</div></div></div>})}</div>; }
function LoadingState() { return <div className="min-h-screen p-6 md:p-10"><div className="skeleton h-4 w-44 rounded"/><div className="skeleton mt-4 h-14 max-w-xl rounded-xl"/><div className="skeleton mt-9 h-48 rounded-3xl"/><div className="mt-6 grid gap-4 md:grid-cols-2">{[1,2,3,4].map(item=><div key={item} className="skeleton h-32 rounded-2xl"/>)}</div></div>; }
