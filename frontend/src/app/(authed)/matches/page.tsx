"use client";
import { Dialog } from "@/components/ui/Dialog";


import { useCallback, useEffect, useRef, useState } from "react";
import { api, MatchDto, MatchFormatDto, PlayerDto, TeamDto, SquadSummaryDto } from "@/lib/api";
import {CricketMatchPanel} from "@/components/CricketMatchPanel";
import { useMatchUpdates } from "@/lib/use-match-updates";
import { ErrorState } from "@/components/ui";
import { useNavigation } from "@/lib/navigation";
import { useChampionship } from "@/lib/championship";
import Link from "next/link";
import { toast } from "sonner";

import { Calendar, Circle, Check, ChevronRight, Plus, Minus, Trash2, RefreshCw, Lock } from "lucide-react";

export default function MatchesPage() {
  const role = useNavigation((state) => state.role);
  const active = useChampionship((state) => state.active);
  const championshipId = active?.id;
  const isAdmin = role === "CHAMPIONSHIP_ADMIN";
  const [matches, setMatches] = useState<MatchDto[]>([]);
  const [teams, setTeams] = useState<TeamDto[]>([]);
  const [players, setPlayers] = useState<PlayerDto[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [loading, setLoading] = useState(true);
  const [squadsReady, setSquadsReady] = useState(false);
  const selectedIdRef = useRef<string | null>(null);
  useEffect(() => { selectedIdRef.current = selectedId; }, [selectedId]);

  const [error, setError] = useState("");
  const pending = useRef<AbortController | null>(null);
  const load = useCallback(async () => {
    if (!championshipId) return;
    pending.current?.abort(); const controller = new AbortController(); pending.current = controller;
    const base = `/championships/${championshipId}`;
    try {
      const [m,t,p,squad] = await Promise.all([
        api.get<MatchDto[]>(base+"/matches", { signal:controller.signal }),
        api.get<TeamDto[]>(base+"/teams", { signal:controller.signal }),
        api.get<PlayerDto[]>(base+"/players", { signal:controller.signal }),
        api.get<SquadSummaryDto>(base+"/squad-confirmations", { signal:controller.signal }),
      ]);
      if (controller.signal.aborted) return;
      setMatches(m.data); setTeams(t.data); setPlayers(p.data); setSquadsReady(squad.data.allSquadsLocked); setError("");
      if (!m.data.some(match=>match.id===selectedIdRef.current)) setSelectedId(m.data[0]?.id || null);
    } catch (e: unknown) { if (!controller.signal.aborted) setError((e as {response?:{data?:{message?:string}}})?.response?.data?.message || "Could not load championship matches."); }
    finally { if (!controller.signal.aborted) setLoading(false); }
  }, [championshipId]);
  useEffect(() => { const timer = setTimeout(() => void load(), 0); return () => { clearTimeout(timer); pending.current?.abort(); }; }, [load]);
  const connection = useMatchUpdates(load);
  const selected = matches.find((m) => m.id === selectedId);

  return (
    <div className="portal-page">
      {error && <ErrorState description={error} onRetry={() => void load()} />}
      <div className="flex items-end justify-between mb-8 flex-wrap gap-4">
        <div>
          <div className="label-cap">Fixtures · {connection === "live" ? "Live updates" : "Reconnecting"}</div>
          <h1 className="h-heading text-5xl font-bold mt-1" data-testid="matches-heading">Matches</h1>
          <p className="text-white/50 mt-2 max-w-xl">League and playoff fixtures with backend-validated sport formats.</p>
        </div>
        <div className="flex items-center gap-3">
          <button className="btn btn-ghost" onClick={() => load()} data-testid="refresh-matches-btn" title="Refresh matches">
            <RefreshCw size={14} className={loading ? "animate-spin" : ""} />
            Refresh
          </button>
          {isAdmin && (
            <button className="btn btn-primary" disabled={!squadsReady} onClick={() => setShowCreate(true)} data-testid="new-match-btn" title={squadsReady?"Schedule a league match":"Lock every squad in Phase 2 first"}>
              <Plus size={14} /> New Match
            </button>
          )}
        </div>
      </div>

      {!squadsReady && <div className="mb-6 flex flex-col gap-3 rounded-2xl border border-warning/25 bg-warning/[.07] p-5 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-start gap-3"><Lock className="mt-0.5 shrink-0 text-warning" size={18}/><div><div className="h-heading text-sm text-warning">League scheduling is locked</div><p className="mt-1 text-xs text-white/45">Every captain must confirm their roster and the championship administrator must lock all squads.</p></div></div>{isAdmin&&<Link href="/squad-confirmation" className="btn btn-ghost shrink-0 justify-center">Open Phase 2<ChevronRight size={14}/></Link>}</div>}

      {matches.length === 0 && (
        <div className="card-elev rounded-2xl p-16 text-center">
          <div className="label-cap">Empty</div>
          <div className="h-heading text-2xl mt-2 text-white/60">No matches scheduled yet</div>
          <p className="text-white/40 mt-2">
            {isAdmin ? (squadsReady ? "Click 'New Match' to schedule a fixture." : "Complete Squad Confirmation to unlock league fixtures.") : "Fixtures will appear here once scheduled."}
          </p>
        </div>
      )}

      <div className="grid grid-cols-12 gap-6">
        {/* Sidebar list */}
        <div className="col-span-12 lg:col-span-4">
          <div className="space-y-2" data-testid="match-list">
            {matches.map((m) => (
              <button
                key={m.id}
                onClick={() => setSelectedId(m.id)}
                className={`w-full text-left p-4 rounded-xl border transition-colors ${
                  selectedId === m.id ? "bg-primary/5 border-primary" : "card-elev hover:border-white/30"
                }`}
                data-testid={`match-item-${m.matchNumber}`}
              >
                <div className="flex items-center justify-between">
                  <div className="label-cap text-[10px]">Match #{m.matchNumber}</div>
                  <MatchStatusChip status={m.status} />
                </div>
                <div className="flex items-center justify-between mt-2">
                  <div className="flex items-center gap-2">
                    <TeamBadge team={m.teamA} />
                    <span className="text-white/40 text-xs">vs</span>
                    <TeamBadge team={m.teamB} />
                  </div>
                  {m.status === "COMPLETED" && (
                    <div className="h-heading text-lg">{m.teamAFormatWins}-{m.teamBFormatWins}</div>
                  )}
                </div>
                {m.venue && <div className="text-white/40 text-[11px] mt-1">{m.venue}</div>}
              </button>
            ))}
          </div>
        </div>

        {/* Detail */}
        {selected && (
          <div className="col-span-12 lg:col-span-8 space-y-4">
            <MatchHeader match={selected} isAdmin={isAdmin} onDelete={async () => {
              if (!confirm("Delete this match?")) return;
              await api.delete(`/admin/matches/${selected.id}`);
              toast.success("Match deleted");
              setSelectedId(null);
              await load();
            }} />
            {active?.sportType.toLowerCase()==="cricket"?<CricketMatchPanel championshipId={active.id} match={selected} players={players} canManage={isAdmin}/>:<div className="space-y-3">
              {selected.formats.map((f) => (
                <FormatCard
                  key={f.id}
                  match={selected}
                  format={f}
                  players={players}
                  isAdmin={isAdmin}
                  onChange={load}
                />
              ))}
            </div>}
          </div>
        )}
      </div>

      {showCreate && (
        <CreateMatchDialog teams={teams} onClose={() => setShowCreate(false)} onCreated={async () => { setShowCreate(false); await load(); }} />
      )}
    </div>
  );
}

function MatchStatusChip({ status }: { status: string }) {
  const s = status === "LIVE" ? "chip chip-live" : status === "COMPLETED" ? "chip chip-primary" : "chip";
  return <span className={s}>{status}</span>;
}

function TeamBadge({ team }: { team: MatchDto["teamA"] }) {
  return (
    <span className="inline-flex items-center gap-2">
      <span className="w-6 h-6 rounded flex items-center justify-center h-heading text-[10px] font-bold"
            style={{ background: team.primaryColor + "22", color: team.primaryColor, border: `1px solid ${team.primaryColor}55` }}>
        {team.shortCode}
      </span>
      <span className="text-sm">{team.name}</span>
    </span>
  );
}

function MatchHeader({ match, isAdmin, onDelete }: { match: MatchDto; isAdmin: boolean; onDelete: () => void }) {
  return (
    <div className="card-elev rounded-2xl p-6">
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <div className="label-cap">Match #{match.matchNumber}</div>
          <div className="h-heading text-3xl mt-1 flex items-center gap-3 flex-wrap">
            {match.teamA.name}
            <span className="text-white/30 text-xl">vs</span>
            {match.teamB.name}
          </div>
          {match.venue && <div className="text-white/50 text-sm mt-1 flex items-center gap-1"><Calendar size={12} /> {match.venue}</div>}
        </div>
        <div className="flex flex-col items-end gap-2">
          <MatchStatusChip status={match.status} />
          {match.status === "COMPLETED" && (
            <div>
              <div className="stat-num text-4xl text-primary">{match.teamAFormatWins} - {match.teamBFormatWins}</div>
              {isAdmin && <div className="label-cap text-[10px] text-right">Winner: {match.winner?.name || "—"}</div>}
            </div>
          )}
          {isAdmin && (
            <button className="btn btn-ghost text-xs" onClick={onDelete} data-testid="delete-match">
              <Trash2 size={12} /> Delete
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

function FormatCard({ match, format, players, isAdmin, onChange }: {
  match: MatchDto; format: MatchFormatDto; players: PlayerDto[]; isAdmin: boolean; onChange: () => Promise<void>;
}) {
  const [assign, setAssign] = useState(false);
  const [score, setScore] = useState(false);
  const [sideA, setSideA] = useState<string[]>(format.sideAPlayers.map((p) => p.id));
  const [sideB, setSideB] = useState<string[]>(format.sideBPlayers.map((p) => p.id));
  const [sA, setSA] = useState(format.scoreA);
  const [sB, setSB] = useState(format.scoreB);
  const [submitting, setSubmitting] = useState(false);

  const handleScoreChange = (newSA: number, newSB: number) => {
    setSA(newSA);
    setSB(newSB);
    // Fire and forget live score update to backend so all viewers see it instantly
    if (!format.completed) {
      api.post(`/admin/matches/formats/${format.id}/live-score`, { scoreA: newSA, scoreB: newSB }).catch(console.error);
    }
  };

  const teamAPlayers = players.filter((p) => p.teamId === match.teamA.id);
  const teamBPlayers = players.filter((p) => p.teamId === match.teamB.id);

  const need = format.playersPerSide;
  const filter = (t: string) =>
    (t === "A" ? teamAPlayers : teamBPlayers).filter((p) => {
      if (format.genderRule === "MALE") return p.gender === "MALE";
      if (format.genderRule === "FEMALE") return p.gender === "FEMALE";
      return true;
    });

  const submitAssign = async () => {
    try {
      await api.post(`/admin/matches/formats/${format.id}/assign`, { sideAPlayerIds: sideA, sideBPlayerIds: sideB });
      toast.success("Players assigned");
      setAssign(false);
      await onChange();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || e?.response?.data || "Failed");
    }
  };
  const submitScore = async () => {
    if (submitting) return;
    setSubmitting(true);
    try {
      if (format.completed) await api.put(`/admin/matches/formats/${format.id}/result`, { scoreA: sA, scoreB: sB });
      else await api.post(`/admin/matches/formats/${format.id}/result`, { scoreA: sA, scoreB: sB });
      toast.success(format.completed ? "Result correction recorded" : "Result recorded");
      setScore(false);
      await onChange();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || e?.response?.data || "Failed");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={`card-elev rounded-xl p-5 border ${format.completed ? "border-primary/30" : "border-white/10"}`} data-testid={`format-card-${format.formatOrder}`}>
      <div className="flex items-start justify-between mb-3">
        <div>
          <div className="flex items-center gap-2">
            <span className="chip chip-primary">{format.shortLabel}</span>
            <span className="h-heading text-lg">{format.label}</span>
          </div>
          <div className="label-cap text-[10px] mt-1">Format #{format.formatOrder}</div>
        </div>
        {format.completed ? (
          <div className="text-right">
            <div className="stat-num text-2xl text-primary">{format.scoreA} - {format.scoreB}</div>
            {isAdmin && <div className="label-cap text-[10px] mt-1">Winner: {format.winner?.shortCode}</div>}
          </div>
        ) : format.sideAPlayers.length > 0 && format.sideBPlayers.length > 0 ? (
          <div className="text-right">
            <div className="stat-num text-2xl text-white/80">{format.scoreA} - {format.scoreB}</div>
            <div className="label-cap text-[10px] mt-1 text-danger animate-pulse">Live</div>
          </div>
        ) : (
          <span className="chip">Pending</span>
        )}
      </div>
      <div className="grid grid-cols-2 gap-3 text-sm">
        <div className="border border-white/10 rounded-lg p-3">
          <div className="label-cap mb-2 text-[9px]" style={{ color: match.teamA.primaryColor }}>{match.teamA.shortCode}</div>
          {format.sideAPlayers.length > 0 ? format.sideAPlayers.map((p) => (
            <div key={p.id} className="text-white/80 text-xs" data-testid={`p-a-${p.id}`}>{p.fullName}</div>
          )) : <div className="text-white/30 text-xs italic">— not assigned —</div>}
        </div>
        <div className="border border-white/10 rounded-lg p-3">
          <div className="label-cap mb-2 text-[9px]" style={{ color: match.teamB.primaryColor }}>{match.teamB.shortCode}</div>
          {format.sideBPlayers.length > 0 ? format.sideBPlayers.map((p) => (
            <div key={p.id} className="text-white/80 text-xs" data-testid={`p-b-${p.id}`}>{p.fullName}</div>
          )) : <div className="text-white/30 text-xs italic">— not assigned —</div>}
        </div>
      </div>
      {isAdmin && (
        <div className="flex gap-2 mt-3">
          {!format.completed ? (
            <>
              <button className="btn btn-ghost text-xs" onClick={() => setAssign(!assign)} data-testid={`assign-${format.formatOrder}`}>
                {format.sideAPlayers.length ? "Change players" : "Assign players"}
              </button>
              {format.sideAPlayers.length > 0 && format.sideBPlayers.length > 0 && (
                <button className="btn btn-primary text-xs" onClick={() => setScore(!score)} data-testid={`record-${format.formatOrder}`}>
                  Record result
                </button>
              )}
            </>
          ) : <button className="btn btn-ghost text-xs" onClick={() => setScore(!score)} data-testid={`correct-${format.formatOrder}`}>Correct result</button>}
        </div>
      )}
      {assign && (
        <div className="mt-4 p-4 rounded-lg border border-white/10 bg-black/30 space-y-3">
          <PlayerPicker label={`${match.teamA.shortCode} side (need ${need})`} options={filter("A")} value={sideA} setValue={setSideA} max={need} />
          <PlayerPicker label={`${match.teamB.shortCode} side (need ${need})`} options={filter("B")} value={sideB} setValue={setSideB} max={need} />
          <button className="btn btn-primary text-xs" onClick={submitAssign} data-testid={`submit-assign-${format.formatOrder}`}>
            <Check size={12} /> Save assignment
          </button>
        </div>
      )}
      {score && (
        <div className="mt-4 p-6 rounded-2xl border border-white/10 bg-black/40">
          <div className="text-center mb-6">
             <div className="label-cap text-white/50">Update Match Score</div>
          </div>
          <div className="flex items-center justify-center gap-6 sm:gap-12 mb-8">
            {/* Team A */}
            <div className="flex flex-col items-center">
              <div className="label-cap mb-4 text-base" style={{ color: match.teamA.primaryColor }}>{match.teamA.shortCode}</div>
              <div className="flex items-center gap-3 sm:gap-5">
                <button
                  className="w-10 h-10 sm:w-12 sm:h-12 rounded-full border border-white/20 flex items-center justify-center hover:bg-white/10 transition-colors text-white/70 hover:text-white"
                  onClick={() => handleScoreChange(Math.max(0, sA - 1), sB)}
                >
                  <Minus size={20} />
                </button>
                <div className="stat-num text-5xl sm:text-6xl w-16 sm:w-20 text-center text-white">{sA}</div>
                <button
                  className="w-10 h-10 sm:w-12 sm:h-12 rounded-full border border-primary/40 bg-primary/10 flex items-center justify-center hover:bg-primary/20 transition-colors text-primary"
                  onClick={() => handleScoreChange(sA + 1, sB)}
                >
                  <Plus size={20} />
                </button>
              </div>
            </div>

            <div className="text-white/20 text-3xl font-light italic mt-8">vs</div>

            {/* Team B */}
            <div className="flex flex-col items-center">
              <div className="label-cap mb-4 text-base" style={{ color: match.teamB.primaryColor }}>{match.teamB.shortCode}</div>
              <div className="flex items-center gap-3 sm:gap-5">
                <button
                  className="w-10 h-10 sm:w-12 sm:h-12 rounded-full border border-white/20 flex items-center justify-center hover:bg-white/10 transition-colors text-white/70 hover:text-white"
                  onClick={() => handleScoreChange(sA, Math.max(0, sB - 1))}
                >
                  <Minus size={20} />
                </button>
                <div className="stat-num text-5xl sm:text-6xl w-16 sm:w-20 text-center text-white">{sB}</div>
                <button
                  className="w-10 h-10 sm:w-12 sm:h-12 rounded-full border border-primary/40 bg-primary/10 flex items-center justify-center hover:bg-primary/20 transition-colors text-primary"
                  onClick={() => handleScoreChange(sA, sB + 1)}
                >
                  <Plus size={20} />
                </button>
              </div>
            </div>
          </div>
          <button className="btn btn-primary mx-auto justify-center" disabled={submitting||sA===sB} onClick={submitScore}>
            {submitting ? "Saving…" : format.completed ? "Confirm audited correction" : "Confirm final result"}
          </button>
        </div>
      )}
    </div>
  );
}

function PlayerPicker({ label, options, value, setValue, max }: { label: string; options: PlayerDto[]; value: string[]; setValue: (v: string[]) => void; max: number }) {
  const toggle = (id: string) => {
    if (value.includes(id)) setValue(value.filter((x) => x !== id));
    else if (value.length < max) setValue([...value, id]);
  };
  return (
    <div>
      <div className="label-cap mb-2">{label}</div>
      <div className="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto">
        {options.map((p) => {
          const sel = value.includes(p.id);
          return (
            <button
              key={p.id}
              type="button"
              onClick={() => toggle(p.id)}
              className={`chip text-[10px] ${sel ? "chip-primary" : ""}`}
              data-testid={`pick-${p.id}`}
            >
              {p.fullName} · {p.gender[0]}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function CreateMatchDialog({ teams, onClose, onCreated }: { teams: TeamDto[]; onClose: () => void; onCreated: () => void }) {
  const [teamA, setTeamA] = useState(teams[0]?.id || "");
  const [teamB, setTeamB] = useState(teams[1]?.id || "");
  const [when, setWhen] = useState(new Date().toISOString().slice(0, 16));
  const [venue, setVenue] = useState("Center Court");
  const submit = async () => {
    if (teamA === teamB) return toast.error("Teams must differ");
    try {
      await api.post("/admin/matches", { teamAId: teamA, teamBId: teamB, scheduledAt: new Date(when).toISOString(), venue });
      toast.success("Match created");
      onCreated();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Failed");
    }
  };
  return (
    <Dialog title="Schedule Match" description="Fixtures" onClose={onClose}>
        <div className="space-y-3">
          <div>
            <label htmlFor="new-team-a" className="field-label">Team A</label>
            <select className="input" value={teamA} onChange={(e) => setTeamA(e.target.value)} id="new-team-a" data-testid="new-team-a">
              {teams.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </div>
          <div>
            <label htmlFor="new-team-b" className="field-label">Team B</label>
            <select className="input" value={teamB} onChange={(e) => setTeamB(e.target.value)} id="new-team-b" data-testid="new-team-b">
              {teams.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </div>
          <div>
            <label htmlFor="new-when" className="field-label">Scheduled at</label>
            <input type="datetime-local" className="input" value={when} onChange={(e) => setWhen(e.target.value)} id="new-when" data-testid="new-when" />
          </div>
          <div>
            <label htmlFor="new-venue" className="field-label">Venue</label>
            <input className="input" value={venue} onChange={(e) => setVenue(e.target.value)} id="new-venue" data-testid="new-venue" />
          </div>
          <div className="flex gap-2 pt-2">
            <button className="btn btn-ghost flex-1 justify-center" onClick={onClose}>Cancel</button>
            <button className="btn btn-primary flex-1 justify-center" onClick={submit} data-testid="submit-new-match">Create</button>
          </div>
        </div>
    </Dialog>
  );
}
