"use client";

import { useState } from "react";
import { api, StandingDto } from "@/lib/api";
import { useChampionship } from "@/lib/championship";
import { useMatchUpdates } from "@/lib/use-match-updates";
import { useTenantResource } from "@/lib/use-resource";
import { ErrorState, SearchInput } from "@/components/ui";
import { PortalLoading } from "@/components/portal/Portal";
import { Trophy, RefreshCw, AlertTriangle } from "lucide-react";

export default function ScoreboardPage() {
  const cricket = useChampionship(s => s.active?.sportType) === "CRICKET";
  const [penaltiesOn,setPenaltiesOn]=useState(true);
  const [search,setSearch]=useState("");
  const standings=useTenantResource<StandingDto[]>('/analytics/standings?penalties='+penaltiesOn);
  const rows=(standings.data || []).filter(row=>row.team.name.toLowerCase().includes(search.toLowerCase()));
  const loading=standings.loading;
  const load=standings.refresh;
  const connection = useMatchUpdates(load);
  if(loading)return <PortalLoading/>;
  if(standings.error)return <ErrorState description={standings.error} onRetry={load}/>;

  const anyPenalty = rows.some((r) => r.penalty > 0);
  const noMatches = rows.every((r) => r.played === 0);

  return (
    <div className="portal-page">
      <div className="flex items-end justify-between mb-8 flex-wrap gap-4">
        <div>
          <div className="label-cap">League Table · {connection === "live" ? "Live updates" : "Reconnecting · periodic refresh active"}</div>
          <h1 className="h-heading text-5xl font-bold mt-1" data-testid="scoreboard-heading">Score Board</h1>
          <p className="text-white/50 mt-2 max-w-xl">
            Live standings follow the configured championship sport rules.
          </p>
        </div>
        <div className="flex gap-2">
          <label className="chip cursor-pointer" data-testid="toggle-penalties">
            <input
              type="checkbox"
              checked={penaltiesOn}
              onChange={(e) => setPenaltiesOn(e.target.checked)}
              className="accent-primary"
            />
            Apply configured participation penalties
          </label>
          <button className="btn btn-ghost" onClick={() => load()} data-testid="refresh-standings">
            <RefreshCw size={14} /> Refresh
          </button>
        </div>
      </div>

      <SearchInput label="Search standings" placeholder="Find a team…" value={search} onChange={e=>setSearch(e.target.value)} className="mb-5 max-w-sm" />
      {noMatches && (
        <div className="card-elev rounded-2xl p-8 mb-6 flex items-center gap-4">
          <AlertTriangle className="text-warning" />
          <div>
            <div className="h-heading text-lg">No completed matches yet</div>
            <div className="text-white/50 text-sm mt-1">
              Waiting for the tournament to begin. Standings appear once matches are completed.
            </div>
          </div>
        </div>
      )}

      <div className="card-elev rounded-2xl overflow-hidden">
        <div className="table-scroll" role="region" aria-label="Championship records" tabIndex={0}><table className="w-full text-sm data-table" data-testid="standings-table">
          <thead>
            <tr className="text-left label-cap text-[10px] bg-white/5">
              <th scope="col" className="py-4 pl-6 pr-4 w-14">#</th>
              <th scope="col" className="py-4 pr-4">Team</th>
              <th scope="col" className="py-4 pr-4 text-center">P</th>
              <th scope="col" className="py-4 pr-4 text-center">W</th>
              <th scope="col" className="py-4 pr-4 text-center">L</th>
              {cricket ? <th scope="col" className="py-4 pr-4 text-center">NRR</th> : <><th scope="col" className="py-4 pr-4 text-center">FW</th><th scope="col" className="py-4 pr-4 text-center">FL</th><th scope="col" className="py-4 pr-4 text-center">Diff</th></>}
              <th scope="col" className="py-4 pr-4 text-center">Base</th>
              <th scope="col" className="py-4 pr-4 text-center">Penalty</th>
              <th scope="col" className="py-4 pr-6 text-right">Points</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.team.id} className="border-t border-white/5 hover:bg-white/5 transition-colors" data-testid={`standing-row-${r.team.shortCode}`}>
                <td className="py-4 pl-6 pr-4">
                  <div className={`h-heading text-2xl ${r.rank === 1 ? "text-primary drop-shadow-glow" : ""}`}>{r.rank}</div>
                </td>
                <td className="py-4 pr-4">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-lg flex items-center justify-center h-heading font-bold text-xs"
                         style={{ background: r.team.primaryColor + "22", color: r.team.primaryColor, border: `1px solid ${r.team.primaryColor}55` }}>
                      {r.team.shortCode}
                    </div>
                    <div>
                      <div className="h-heading text-base">{r.team.name}</div>
                      {r.rank === 1 && (
                        <div className="chip chip-primary text-[9px] mt-1">
                          <Trophy size={10} /> Leader
                        </div>
                      )}
                    </div>
                  </div>
                </td>
                <td className="py-4 pr-4 text-center text-white/70">{r.played}</td>
                <td className="py-4 pr-4 text-center h-heading text-primary">{r.won}</td>
                <td className="py-4 pr-4 text-center text-white/70">{r.lost}</td>
                {cricket ? <td className="py-4 pr-4 text-center">{r.netRunRate == null ? "—" : r.netRunRate.toFixed(3)}</td> : <><td className="py-4 pr-4 text-center text-white/70">{r.formatWins}</td><td className="py-4 pr-4 text-center text-white/70">{r.formatLosses}</td><td className="py-4 pr-4 text-center">{r.formatDiff > 0 ? "+" : ""}{r.formatDiff}</td></>}
                <td className="py-4 pr-4 text-center text-white/60">{r.basePoints}</td>
                <td className="py-4 pr-4 text-center">
                  {r.penalty > 0 ? (
                    <span className="chip text-danger border-danger/40 bg-danger/10">-{r.penalty}</span>
                  ) : (
                    <span className="text-white/30">—</span>
                  )}
                </td>
                <td className="py-4 pr-6 text-right">
                  <div className="stat-num text-2xl">{r.totalPoints}</div>
                </td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </div>

      {anyPenalty && (
        <div className="mt-6 p-4 rounded-lg border border-danger/30 bg-danger/5 text-sm text-white/70 flex items-start gap-3" data-testid="penalty-notice">
          <AlertTriangle className="text-danger flex-shrink-0" size={18} />
          <div>
            <div className="h-heading text-danger">Participation penalty applied</div>
            <div>Every squad member must play at least one league match. Teams with unplayed players lose 2 points per absentee.</div>
          </div>
        </div>
      )}
    </div>
  );
}
