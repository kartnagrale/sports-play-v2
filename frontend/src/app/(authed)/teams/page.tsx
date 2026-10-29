"use client";
import { useState } from "react";
import { AuctionStateDto, PlayerDto, TeamDto } from "@/lib/api";
import { useTenantResource } from "@/lib/use-resource";
import { useAuth } from "@/lib/auth";
import { useChampionship } from "@/lib/championship";
import { formatCr } from "@/lib/format";
import { PlayerIdentity, PlayerAvatar } from "@/components/players/PlayerAvatar";
import { Card, DataTable, EmptyState, ErrorState, MetricCard, PageHeader, SectionHeader, SearchInput, Select, StatusBadge } from "@/components/ui";
import { PortalLoading } from "@/components/portal/Portal";
export default function TeamsPage() {
  const teams = useTenantResource<TeamDto[]>("/teams"); const players = useTenantResource<PlayerDto[]>("/players");
  const active = useChampionship(s => s.active); const user = useAuth(s => s.user);
  const rules = useTenantResource<AuctionStateDto>(active?.defaultAuctionId ? `/auctions/${active.defaultAuctionId}/state` : null);
  const own = user?.championships.find(c => c.championshipId === active?.id)?.teamId;
  const [selected,setSelected] = useState(""); const [search,setSearch] = useState(""); const [sort,setSort] = useState("name");
  if (teams.loading || players.loading) return <PortalLoading />;
  if (teams.error || players.error) return <ErrorState description={teams.error || players.error} onRetry={() => { teams.refresh(); players.refresh(); }} />;
  const list = teams.data || []; const team = list.find(t => t.id === selected) || list.find(t => t.id === own) || list[0];
  const roster = (players.data || []).filter(p => p.teamId === team?.id && p.fullName.toLowerCase().includes(search.toLowerCase())).sort((a,b) => sort === "price" ? Number(b.soldPrice || 0) - Number(a.soldPrice || 0) || a.fullName.localeCompare(b.fullName) : a.fullName.localeCompare(b.fullName));
  return <div className="portal-page"><PageHeader eyebrow="Team directory" title="The teams behind the competition" description="Explore squads, player identities and confirmed auction purchases." />
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4 mb-6">{list.map(t => <button key={t.id} onClick={() => setSelected(t.id)} aria-pressed={team?.id === t.id} className={`team-choice ${team?.id === t.id ? "is-selected" : ""}`} data-testid={`team-tab-${t.shortCode}`}><PlayerAvatar name={t.name} imageUrl={t.logoUrl} shape="square" /><span className="min-w-0"><span className="block font-semibold">{t.name}</span><span className="mt-1 block text-xs text-muted">{t.totalPlayers} players · {t.matchPoints} pts</span></span></button>)}</div>
    {!team ? <EmptyState title="No teams yet" description="Teams will appear once they are registered for the championship." /> : <><SectionHeader title={team.name} eyebrow={team.id === own ? "Your team" : "Team profile"} actions={<StatusBadge tone="info">{team.shortCode}</StatusBadge>} /><div className="portal-metrics"><MetricCard label="Squad size" value={`${team.totalPlayers}${rules.data ? ` / ${rules.data.maxSquadSize}` : ""}`} /><MetricCard label="Purse remaining" value={formatCr(Number(team.purseRemaining))} /><MetricCard label="Invested in players" value={formatCr(Number(team.purseTotal) - Number(team.purseRemaining))} /><MetricCard label="Match points" value={team.matchPoints} /></div>
    {rules.data&&<Card className="my-4 text-sm text-muted">Premium purchases: {rules.data.teamEligibility?.find(e=>e.teamId===team.id)?.premiumPurchases??0}/{rules.data.maximumPremiumPurchases??"Unlimited"} · Female players: {team.femaleCount}/{rules.data.minFemale} minimum · Squad slots remaining: {Math.max(0,rules.data.maxSquadSize-team.totalPlayers)}. Captain counts toward the squad and gender requirements.</Card>}
    <div className="portal-toolbar"><SearchInput label="Search roster" placeholder="Search this squad…" value={search} onChange={e => setSearch(e.target.value)} /><Select aria-label="Sort roster" value={sort} onChange={e => setSort(e.target.value)}><option value="name">Player name</option><option value="price">Highest purchase first</option></Select></div><Card className="p-0 overflow-hidden"><DataTable caption={`${team.name} player roster`}><thead><tr>{["Player","Category","Skill","Base price","Purchase price"].map(title => <th key={title} scope="col">{title}</th>)}</tr></thead><tbody>{roster.map(p => <tr key={p.id} data-testid={`row-player-${p.id}`}><td><PlayerIdentity name={p.fullName} imageUrl={p.photoUrl} /></td><td>{p.gender}</td><td>{p.skillLevel || "—"}</td><td>{formatCr(Number(p.basePrice))}</td><td className="font-mono text-primary">{p.status==="PREASSIGNED"?"Preassigned captain · no purse cost":formatCr(Number(p.soldPrice || 0))}</td></tr>)}</tbody></DataTable>{!roster.length && <EmptyState title={search ? "No matching players" : "No players bought yet"} description="Confirmed purchases appear here after the auction." />}</Card></>}
  </div>;
}
