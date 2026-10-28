"use client";
import { Card, EmptyState } from "@/components/ui";
import { humanize } from "./Portal";
import { formatCr } from "@/lib/format";
import type { MatchDto, TeamDto } from "@/lib/api";
export interface AnalyticsSummary {
  matches: Record<string, number>; players?: Record<string, number>; certificates?: Record<string, number>;
  squads?: Record<string, number>; purses?: { id: string; name: string; allocated: number; remaining: number }[];
  captain?: { team: TeamDto; composition: Record<string, number>; played: number; won: number; lost: number; squadStatus: string; upcoming: MatchDto[]; recent: MatchDto[] };
  asOf: string;
}
export interface PlatformAnalytics { championships: number; participants: number; status: Record<string,number>; sports: Record<string,number>; lifecycle: Record<string,number>; recent: { championship:string; action:string; at:string }[] }
/** Native meters and visible numeric labels convey the same information without color or pointer interaction. */
export function CountChart({ title, values, description }: { title: string; values: Record<string,number>; description?: string }) {
  const entries = Object.entries(values); const total = entries.reduce((sum,[,value])=>sum+value,0);
  return <Card><h2 className="section-title">{title}</h2>{description && <p className="mt-2 text-xs text-muted">{description}</p>}{!total ? <EmptyState title="No activity yet" description="Counts appear when championship records are available." /> : <dl className="mt-5 space-y-4">{entries.map(([key,value])=><div key={key}><div className="mb-2 flex items-center justify-between gap-3 text-sm"><dt>{humanize(key)}</dt><dd className="stat-num">{value.toLocaleString()} <span className="text-xs text-muted">/ {total.toLocaleString()}</span></dd></div><meter aria-label={`${humanize(key)}: ${value} of ${total}`} min={0} max={Math.max(1,total)} value={value} className="h-3 w-full accent-primary" /></div>)}</dl>}</Card>;
}
export function PurseChart({ purses }: { purses: NonNullable<AnalyticsSummary["purses"]> }) {
  return <Card><h2 className="section-title">Team purse allocation</h2><p className="mt-2 text-xs text-muted">Committed auction purchases · server-confirmed balances</p>{!purses.length ? <EmptyState title="No teams yet" /> : <ul className="mt-5 space-y-5">{purses.slice(0,8).map(team=>{const spent=Number(team.allocated)-Number(team.remaining);return <li key={team.id}><div className="mb-2 flex flex-wrap justify-between gap-2 text-sm"><span className="font-medium">{team.name}</span><span>{formatCr(spent)} spent · {formatCr(Number(team.remaining))} left</span></div><meter aria-label={`${team.name}: ${spent} spent of ${team.allocated}`} min={0} max={Math.max(1,Number(team.allocated))} value={spent} className="h-3 w-full accent-primary" /></li>;})}</ul>}{purses.length>8 && <p className="mt-4 text-xs text-muted">Showing 8 teams. Full balances are available in Teams.</p>}</Card>;
}
