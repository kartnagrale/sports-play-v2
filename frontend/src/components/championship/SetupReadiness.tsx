"use client";
import Link from "next/link";
import {useEffect,useState} from "react";
import {api,CompetitionOverviewDto,DashboardCapabilitiesDto,PlayerDto} from "@/lib/api";

/** A small guide on the existing admin screen; every count comes from the selected championship. */
export default function SetupReadiness({championshipId}:{championshipId:string}) {
  const [data,setData]=useState<{competition:CompetitionOverviewDto;dashboard:DashboardCapabilitiesDto;players:PlayerDto[];captains:{playerId?:string}[]}|null>(null);
  const [error,setError]=useState("");
  useEffect(()=>{
    const controller=new AbortController(),options={signal:controller.signal};
    const base=`/championships/${championshipId}`;
    Promise.all([api.get<CompetitionOverviewDto>(base+"/competition",options),api.get<DashboardCapabilitiesDto>(base+"/dashboard",options),api.get<PlayerDto[]>(base+"/players",options),api.get<{playerId?:string}[]>(base+"/admin/captain-players",options)])
      .then(([competition,dashboard,players,captains])=>{if(!controller.signal.aborted)setData({competition:competition.data,dashboard:dashboard.data,players:players.data,captains:captains.data});})
      .catch(()=>{if(!controller.signal.aborted)setError("Readiness could not load. Refresh this page to retry.");});
    return ()=>controller.abort();
  },[championshipId]);
  if(!data)return <p className="my-5 text-sm text-muted" role="status">{error||"Loading championship readiness…"}</p>;
  const {competition,dashboard,players,captains}=data;
  const configured=competition.configuration.configuredTeamCount;
  const expectedLeague=configured*(configured-1)/2*(competition.configuration.roundRobinMode==="SINGLE"?1:competition.configuration.roundRobinMode==="DOUBLE"?2:competition.configuration.numberOfLegs);
  const assigned=captains.filter(c=>c.playerId).length;
  const eligible=players.filter(p=>["AVAILABLE","ON_BLOCK","UNSOLD"].includes(p.status)).length;
  const league=competition.fixtures.filter(f=>f.stageType==="LEAGUE");
  const complete=league.filter(f=>f.status==="COMPLETED").length;
  return <section className="my-6 rounded-2xl border border-border bg-surface p-5" aria-label="Championship setup readiness">
    <h2 className="h-heading text-xl">Setup & readiness</h2>
    <div className="mt-4 grid gap-4 text-sm sm:grid-cols-2 lg:grid-cols-3">
      <div><Link className="text-primary underline" href="/admin/team-management">Teams, captain logins & player assignment</Link><p className="mt-1 text-muted">{configured} teams required. Currently {dashboard.teamCount}/{configured}. Captains in roster: {assigned}/{configured}.</p></div>
      <div><Link className="text-primary underline" href="/players">Player Excel import & photographs</Link><p className="mt-1 text-muted">{players.length} total identities · {eligible} auction eligible · {players.filter(p=>p.status==="PREASSIGNED").length} preassigned. {players.filter(p=>p.photoUrl).length} photographs saved.</p></div>
      <div><Link className="text-primary underline" href="/teams">Team roster & gender progress</Link><p className="mt-1 text-muted">All {competition.configuration.minimumRosterSize} required roster positions must be filled before squad confirmation. Captains count toward gender minimums.</p></div>
      <div><Link className="text-primary underline" href="/auction">Auction settings & readiness</Link><p className="mt-1 text-muted">Finalize the purse, bid limits and roster rules before bidding. Required captain assignments are checked again by the server.</p></div>
      <div><Link className="text-primary underline" href="/squad-confirmation">Squad confirmation</Link><p className="mt-1 text-muted">{dashboard.lockedSquads}/{dashboard.totalSquads} squads locked.</p></div>
      <div><Link className="text-primary underline" href="/competition">Competition & cricket rules</Link><p className="mt-1 text-muted">{competition.configuration.playingSideSize} per playing side. Complete all {league.length||expectedLeague} league fixtures before generating the final. Currently {complete}/{league.length||expectedLeague} completed.</p></div>
    </div>
    {dashboard.blockersForNextPhase.length>0&&<ul className="mt-4 list-disc pl-5 text-sm text-warning">{dashboard.blockersForNextPhase.map(blocker=><li key={blocker}>{blocker}</li>)}</ul>}
  </section>;
}
