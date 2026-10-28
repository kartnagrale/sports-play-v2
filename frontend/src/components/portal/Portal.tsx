"use client";
import Link from "next/link";
import { ArrowRight, CalendarDays, Trophy } from "lucide-react";
import { Card, EmptyState, Skeleton, StatusBadge } from "@/components/ui";
import { useNavigation } from "@/lib/navigation";
import { MatchDto, StandingDto } from "@/lib/api";
export const humanize = (value: string) => value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
export function PortalLoading() { return <div className="portal-page" role="status" aria-label="Loading workspace"><Skeleton className="mb-8 h-12 w-64" /><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{[1,2,3,4].map(i => <Skeleton key={i} className="h-36" />)}</div><Skeleton className="mt-6 h-80" /></div>; }
export function PortalActions({ paths }: { paths: string[] }) {
  const screens = useNavigation(state => state.screens);
  const available = paths.map(path => screens.find(screen => screen.path === path)).filter(Boolean);
  return <div className="portal-actions">{available.map(screen => <Link key={screen.path} href={screen.path} className="portal-action"><span>{screen.label}</span><ArrowRight size={17} aria-hidden="true" /></Link>)}</div>;
}
export function Fixtures({ title, fixtures, empty }: { title: string; fixtures: MatchDto[]; empty: string }) {
  return <Card><div className="flex items-center gap-3"><CalendarDays size={18} className="text-primary" /><h2 className="section-title">{title}</h2></div><div className="mt-5 divide-y divide-border">{fixtures.length ? fixtures.map(match => <div key={match.id} className="fixture-row"><div className="min-w-0"><p className="label-cap">{humanize(match.stageType)} · Match {match.matchNumber}</p><p className="mt-2 font-medium">{match.teamA.name} <span className="text-muted">vs</span> {match.teamB.name}</p><p className="mt-1 text-xs text-muted">{match.venue || "Venue to be confirmed"}</p></div><div className="text-right"><StatusBadge tone={match.status === "LIVE" ? "live" : match.status === "COMPLETED" ? "success" : "neutral"} live={match.status === "LIVE"}>{humanize(match.status)}</StatusBadge><p className="mt-2 text-xs text-muted">{match.status === "COMPLETED" ? (match.formats.length ? `${match.teamAFormatWins} – ${match.teamBFormatWins}` : match.winner ? `${match.winner.name} won` : "Completed") : new Date(match.scheduledAt).toLocaleString(undefined, { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })}</p></div></div>) : <EmptyState title={empty} icon={<CalendarDays size={23} />} />}</div></Card>;
}
export function StandingsPreview({ rows }: { rows: StandingDto[] }) {
  return <Card><div className="mb-5 flex items-center gap-3"><Trophy size={18} className="text-primary" /><h2 className="section-title">Championship standings</h2></div>{rows.length ? <ol className="divide-y divide-border">{rows.slice(0,5).map(row => <li key={row.team.id} className="flex items-center gap-4 py-4"><span className="stat-num text-2xl text-primary">{row.rank}</span><div className="min-w-0 flex-1"><p className="font-medium">{row.team.name}</p><p className="mt-1 text-xs text-muted">{row.won} wins · {row.played} played</p></div><span className="stat-num">{row.totalPoints} <span className="text-xs text-muted">pts</span></span></li>)}</ol> : <EmptyState title="Standings will appear here" description="Results determine the championship table." />}</Card>;
}
