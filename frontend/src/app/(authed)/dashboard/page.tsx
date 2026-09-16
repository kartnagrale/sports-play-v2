"use client";

import {useCallback,useEffect,useState} from "react";
import Link from "next/link";
import {api,DashboardCapabilitiesDto,MatchDto,StandingDto} from "@/lib/api";
import {useAuth} from "@/lib/auth";
import {useChampionship} from "@/lib/championship";
import {AlertTriangle,ArrowRight,CalendarDays,CheckCircle2,Loader2,Trophy,Users} from "lucide-react";

export default function DashboardPage(){
  const {user}=useAuth();const active=useChampionship(state=>state.active);
  const [data,setData]=useState<DashboardCapabilitiesDto|null>(null);
  const [loading,setLoading]=useState(true);
  const load=useCallback(async()=>{if(!active)return;setLoading(true);try{
    const response=await api.get<DashboardCapabilitiesDto>(`/championships/${active.id}/dashboard`);setData(response.data);
  }catch(error){console.warn("dashboard capabilities load failed",error);}finally{setLoading(false);}},[active]);
  useEffect(()=>{load();},[load]);
  if(loading)return <div className="grid min-h-[60vh] place-items-center"><Loader2 className="animate-spin text-primary" size={30}/></div>;
  if(!data)return <div className="p-10"><div className="card-elev rounded-2xl p-10 text-center text-white/45">Dashboard capabilities are unavailable for this championship.</div></div>;
  const phaseProgress=Math.max(0,["CREATION","CONFIGURATION","TEAM_PROVISIONING","PLAYER_REGISTRATION","AUCTION","SQUAD_CONFIRMATION","LEAGUE_STAGE","PLAYOFFS","FINAL","COMPLETED","ARCHIVED"].indexOf(data.currentPhase));
  return <div className="p-5 sm:p-10">
    <div className="mb-8 flex flex-wrap items-end justify-between gap-4"><div><div className="label-cap">{active?.name} · {data.sportCode}</div>
      <h1 className="h-heading mt-1 text-4xl font-bold sm:text-5xl">Welcome, {user?.fullName?.split(" ")[0]||"Guest"}</h1>
      <p className="mt-2 text-sm text-white/50">Championship health, lifecycle progress and sport-specific performance.</p></div>
      {data.permittedActions.length>0&&<Link href="/competition" className="btn btn-primary">Competition control <ArrowRight size={14}/></Link>}</div>

    {data.champion&&<div className="mb-8 rounded-3xl border border-primary/30 bg-primary/[.08] p-7 text-center"><Trophy className="mx-auto text-primary"/><div className="label-cap mt-2">Champion</div><div className="h-heading mt-1 text-3xl text-primary">{data.champion.name}</div><div className="mt-1 text-xs text-white/45">Runner-up · {data.runnerUp?.name}</div></div>}

    <div className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Kpi icon={<CheckCircle2/>} label="Current phase" value={label(data.currentPhase)} sub={`${Math.round(phaseProgress/10*100)}% lifecycle progress`}/>
      <Kpi icon={<Users/>} label="Teams & players" value={`${data.teamCount} · ${data.playerCount}`} sub="Championship-isolated records"/>
      <Kpi icon={<CheckCircle2/>} label="Squads locked" value={`${data.lockedSquads} / ${data.totalSquads}`} sub={data.lockedSquads===data.totalSquads&&data.totalSquads>0?"League gate satisfied":"Phase 2 gate"}/>
      <Kpi icon={<Trophy/>} label="Current leader" value={data.standings[0]?.team.shortCode||"—"} sub={data.standings[0]?.team.name||"League not started"}/>
    </div>

    {data.blockersForNextPhase.length>0&&<div className="mb-8 flex items-start gap-3 rounded-2xl border border-warning/25 bg-warning/[.07] p-5"><AlertTriangle className="mt-0.5 shrink-0 text-warning" size={18}/><div><div className="h-heading text-warning">Required before the next phase</div><div className="mt-2 flex flex-wrap gap-2">{data.blockersForNextPhase.map(item=><span className="chip" key={item}>{item}</span>)}</div></div></div>}

    <div className="grid gap-6 xl:grid-cols-2">
      <section className="card-elev rounded-2xl p-6"><div className="flex items-center justify-between"><div><div className="label-cap">Schedule</div><h2 className="h-heading mt-1 text-2xl">Upcoming fixtures</h2></div><CalendarDays className="text-white/30"/></div>
        <FixtureList fixtures={data.upcomingFixtures} empty="No upcoming fixtures are scheduled."/></section>
      <section className="card-elev rounded-2xl p-6"><div className="label-cap">Latest</div><h2 className="h-heading mt-1 text-2xl">Recently completed</h2>
        <FixtureList fixtures={data.recentFixtures} empty="Completed results will appear here."/></section>
    </div>

    {data.supportedWidgets.includes("TEAM_MATCH_STANDINGS")&&<section className="mt-8 card-elev overflow-hidden rounded-2xl"><div className="flex items-center justify-between p-6"><div><div className="label-cap">Badminton</div><h2 className="h-heading mt-1 text-2xl">Team match standings</h2></div><Link href="/scoreboard" className="btn btn-ghost">Full table <ArrowRight size={13}/></Link></div><Standings rows={data.standings.slice(0,4)}/></section>}
  </div>;
}
function Kpi({icon,label:caption,value,sub}:{icon:React.ReactNode;label:string;value:string;sub:string}){return <div className="card-elev rounded-2xl p-5"><div className="flex items-center justify-between text-white/35"><span className="label-cap">{caption}</span>{icon}</div><div className="stat-num mt-3 text-2xl">{value}</div><div className="mt-1 text-xs text-white/40">{sub}</div></div>}
function FixtureList({fixtures,empty}:{fixtures:MatchDto[];empty:string}){return <div className="mt-4 divide-y divide-white/5">{fixtures.length?fixtures.map(f=><div className="flex items-center justify-between gap-4 py-3" key={f.id}><div><div className="label-cap">{label(f.stageType)} · Match {f.matchNumber}</div><div className="h-heading mt-1">{f.teamA.shortCode} <span className="text-white/25">vs</span> {f.teamB.shortCode}</div></div><div className="text-right text-xs text-white/40">{new Date(f.scheduledAt).toLocaleDateString()}<div className="mt-1">{f.venue}</div></div></div>):<p className="py-6 text-sm text-white/40">{empty}</p>}</div>}
function Standings({rows}:{rows:StandingDto[]}){return <div className="divide-y divide-white/5 border-t border-white/5">{rows.map(row=><div className="grid grid-cols-[3rem_1fr_auto] items-center gap-3 px-6 py-4" key={row.team.id}><span className="stat-num text-xl text-primary">{row.rank}</span><span className="h-heading">{row.team.name}</span><span className="stat-num">{row.totalPoints} pts</span></div>)}</div>}
function label(value:string){return value.replaceAll("_"," ").toLowerCase().replace(/\b\w/g,c=>c.toUpperCase())}
