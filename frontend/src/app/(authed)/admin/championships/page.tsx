"use client";
import { useState } from "react";
import Link from "next/link";
import { Dialog } from "@/components/ui/Dialog";
import { ChampionshipDto } from "@/lib/api";
import { useChampionship } from "@/lib/championship";
import { useResource } from "@/lib/use-resource";
import { Card, EmptyState, ErrorState, MetricCard, PageHeader, Pagination, SearchInput, Select, StatusBadge } from "@/components/ui";
import { humanize, PortalLoading } from "@/components/portal/Portal";
import { CountChart, PlatformAnalytics } from "@/components/portal/Analytics";
import { ArrowRight, Plus, Trophy } from "lucide-react";
export default function ChampionshipsAdminPage() {
  const { data: metrics, loading, error, refresh } = useResource<PlatformAnalytics>("/platform/analytics");
  const data = useChampionship(s => s.championships);
  const [page,setPage] = useState(1);
  const select = useChampionship(state => state.select);
  const [inspected,setInspected] = useState<ChampionshipDto | null>(null);
  const [search,setSearch] = useState(""); const [status,setStatus] = useState("ALL"); const [sport,setSport] = useState("ALL");
  if (loading) return <PortalLoading />;
  if (error) return <ErrorState description={error} onRetry={refresh} />;
  const championships = data || []; const sports = [...new Set(championships.map(c => c.sportType))].sort();
  const visible = championships.filter(c => c.name.toLowerCase().includes(search.toLowerCase()) && (status === "ALL" || c.status === status) && (sport === "ALL" || c.sportType === sport)).sort((a,b) => a.name.localeCompare(b.name));
  return <div className="portal-page"><PageHeader eyebrow="Platform overview" title="Every championship. One view." description="A clear view of your competitions, their lifecycle and the people running them." actions={<Link href="/admin/championship-launchpad" className="btn btn-primary"><Plus size={16} />Launch championship</Link>} />
    <div className="portal-metrics"><MetricCard label="Championships" value={metrics?.championships ?? "—"} detail="Across your platform" /><MetricCard label="Active" value={championships.filter(c => c.status === "ACTIVE").length} detail="Competitions in progress" /><MetricCard label="Completed" value={metrics?.status.COMPLETED || 0} detail="Finalized championships" /><MetricCard label="Registered participants" value={metrics?.participants ?? "—"} detail="Registration records across championships" /></div>
    {metrics && <div className="mb-6 grid gap-5 lg:grid-cols-2"><CountChart title="Sports distribution" values={metrics.sports} /><CountChart title="Championship lifecycle" values={metrics.lifecycle} /></div>}
    {metrics && <Card className="mb-6"><h2 className="section-title">Recent platform activity</h2>{metrics.recent.length ? <ul className="mt-4 divide-y divide-border">{metrics.recent.map((item,index)=><li key={index} className="flex flex-wrap justify-between gap-2 py-3 text-sm"><span>{item.championship} · {humanize(item.action)}</span><time className="text-xs text-muted" dateTime={item.at}>{new Date(item.at).toLocaleString()}</time></li>)}</ul> : <p className="mt-4 text-sm text-muted">Competition activity will appear after configuration or scoring changes.</p>}</Card>}
    <div className="portal-toolbar"><SearchInput label="Search championships" placeholder="Find a championship…" value={search} onChange={e => { setSearch(e.target.value); setPage(1); }} /><Select aria-label="Filter championship status" value={status} onChange={e => { setStatus(e.target.value); setPage(1); }}>{["ALL","DRAFT","ACTIVE","COMPLETED","ARCHIVED"].map(s => <option key={s} value={s}>{s === "ALL" ? "All statuses" : humanize(s)}</option>)}</Select><Select aria-label="Filter sport" value={sport} onChange={e => { setSport(e.target.value); setPage(1); }}><option value="ALL">All sports</option>{sports.map(s => <option key={s}>{s}</option>)}</Select></div>
    <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">{visible.slice((page-1)*12,page*12).map(c => <button key={c.id} onClick={() => { setInspected(c); }} className="championship-card"><div className="flex items-center justify-between gap-3"><Trophy className="text-primary" size={24} /><StatusBadge tone={c.status === "ACTIVE" ? "live" : c.status === "COMPLETED" ? "success" : "neutral"}>{humanize(c.status)}</StatusBadge></div><p className="label-cap mt-7">{humanize(c.sportType)}</p><h2 className="section-title mt-2">{c.name}</h2><p className="mt-3 text-sm text-muted">{humanize(c.lifecyclePhase)}</p><div className="mt-6 flex items-center justify-between border-t border-border pt-4"><span className="text-xs text-muted">Room · <span className="font-mono text-primary">{c.roomCode}</span></span><ArrowRight size={17} /></div></button>)}</div>
    <Pagination page={page} pageCount={Math.ceil(visible.length/12)} onPageChange={setPage} />
    {inspected && <Dialog title={inspected.name} description="Championship overview" onClose={()=>setInspected(null)}><dl className="grid grid-cols-2 gap-5 text-sm"><div><dt className="label-cap">Sport</dt><dd className="mt-2">{humanize(inspected.sportType)}</dd></div><div><dt className="label-cap">Status</dt><dd className="mt-2">{humanize(inspected.status)}</dd></div><div><dt className="label-cap">Lifecycle</dt><dd className="mt-2">{humanize(inspected.lifecyclePhase)}</dd></div><div><dt className="label-cap">Room</dt><dd className="mt-2 font-mono">{inspected.roomCode}</dd></div></dl><p className="mt-6 rounded-lg border border-border p-4 text-sm text-muted">Championship configuration and operations belong to its assigned championship administrator. Your platform account manages the championship portfolio and provisioning.</p><button type="button" className="btn btn-primary mt-5 w-full" onClick={()=>{select(inspected);setInspected(null);}}>Use championship context</button></Dialog>}
    {!visible.length && <EmptyState title="No matching championships" description="Try a different search or filter, or launch a new championship." />}
  </div>;
}
