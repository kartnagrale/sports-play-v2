"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useChampionship } from "@/lib/championship";
import { toast } from "sonner";
import { BadgeIndianRupee, CheckCircle2, Clock3, Save, Settings2, ShieldCheck, Users2 } from "lucide-react";

import { ChampionshipSettings as Settings, SettingsForm, settingsError, settingsRequest, inputNumber } from "@/lib/championship-settings";

const empty: SettingsForm = { auctionName: "", maxSquadSize: 12, purseLimit: 1000000000, minMale: 9, minFemale: 3, playerBasePrice: 2000000, bidIncrement: 500000, timerSeconds: 30, customRules: "", version: '', maximumBid:null,maximumPremiumPurchases:null,captainPreassignmentRequired:false };

export default function ChampionshipSettingsPage() {
  const user = useAuth((state) => state.user);
  const { active, load } = useChampionship();
  const championshipId=active?.id;
  const [form, setForm] = useState<SettingsForm>(empty);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const role = user?.championships.find((item) => item.championshipId === active?.id)?.role;
  const allowed = role === "CHAMPIONSHIP_ADMIN";
  const validationError = useMemo(() => settingsError(form), [form]);
  const valid = !validationError;

  useEffect(() => {
    if (!championshipId) return;
    const controller=new AbortController();

    api.get<Settings>(`/championships/${championshipId}/settings`,{signal:controller.signal})
      .then(({ data }) => {if(!controller.signal.aborted)setForm(data);})
      .catch((error) => {if(!controller.signal.aborted)toast.error(error?.response?.data?.message || "Unable to load settings");})
      .finally(() => {if(!controller.signal.aborted)setLoading(false);});
    return ()=>controller.abort();
  }, [championshipId]);

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!active || !valid) return toast.error("Check roster and auction values");
    setSaving(true);
    try {
      const {data} = await api.put<Settings>(`/championships/${active.id}/settings`, settingsRequest(form));
      setForm(data);
      await load();
      toast.success("Championship configured and activated");
    } catch (error: any) { toast.error(error?.response?.data?.message || "Configuration failed"); }
    finally { setSaving(false); }
  }

  if (!allowed) return <div className="portal-page"><h1 className="h-heading text-3xl">Championship Admin access required</h1></div>;
  if (!active || loading) return <div className="p-10 label-cap">Loading championship configuration…</div>;

  return <div className="portal-page"><div className="mx-auto max-w-6xl">
    <div className="mb-8 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between"><div><div className="label-cap text-primary">Championship command centre</div><h1 className="h-heading mt-2 text-4xl font-bold md:text-5xl">Auction & roster settings</h1><p className="mt-3 max-w-2xl text-sm text-white/50">Configure how <span className="text-white">{active.name}</span> will run. Saving a draft creates its auction and activates the workspace.</p></div><span className={`chip ${active.status === "ACTIVE" ? "chip-primary" : ""}`}><CheckCircle2 size={13}/>{active.status}</span></div>
    {active.sportType.toUpperCase()==="CRICKET"&&<div className="mb-5 rounded-xl border border-border p-4 text-sm text-muted"><button type="button" className="btn btn-secondary" onClick={()=>setForm({...form,purseLimit:100000,playerBasePrice:5000,bidIncrement:1000,maximumBid:15000,maximumPremiumPurchases:4,maxSquadSize:11,minMale:0,minFemale:3,captainPreassignmentRequired:true})}>Fill proposed NPL auction rules</button><p className="mt-2">Four teams, eleven members including each preassigned captain. ₹1,000 increment is a proposed default. Save here, then configure the four-team cricket format in Competition.</p></div>}
    <form onSubmit={save} className="grid gap-6 lg:grid-cols-[1.1fr_.9fr]">
      <section className="card-elev rounded-2xl p-6 md:p-7"><Header icon={<Settings2 size={19}/>} title="Auction controls" subtitle="Commercial rules used for every bid"/><div className="space-y-4"><Field label="Auction name"><input className="input" required value={form.auctionName} onChange={e=>setForm({...form,auctionName:e.target.value})}/></Field><div className="grid gap-4 sm:grid-cols-2"><Money label="Team purse" value={form.purseLimit} onChange={purseLimit=>setForm({...form,purseLimit})}/><Money label="Player base price" value={form.playerBasePrice} onChange={playerBasePrice=>setForm({...form,playerBasePrice})}/><Money label="Bid increment" value={form.bidIncrement} onChange={bidIncrement=>setForm({...form,bidIncrement})}/><Field label="Maximum bid (₹, optional)"><input className="input" type="number" min={0.01} step={0.01} value={form.maximumBid??""} onChange={e=>setForm({...form,maximumBid:inputNumber(e.target.value)})}/></Field><Field label="Purchases allowed at maximum bid"><input className="input" type="number" min={0} step={1} value={form.maximumPremiumPurchases??""} onChange={e=>setForm({...form,maximumPremiumPurchases:inputNumber(e.target.value)})}/></Field><Field label="Bid timer (seconds)"><input className="input" type="number" required min={5} max={300} step={1} value={form.timerSeconds} onChange={e=>setForm({...form,timerSeconds:inputNumber(e.target.value)})}/></Field></div><label className="flex gap-2 text-sm text-muted"><input type="checkbox" checked={!!form.captainPreassignmentRequired} onChange={e=>setForm({...form,captainPreassignmentRequired:e.target.checked})}/>Require a preassigned captain in every team before auction</label><Field label="Additional rules"><textarea className="input min-h-28 resize-y" value={form.customRules || ""} onChange={e=>setForm({...form,customRules:e.target.value})} placeholder="Tie-breaks, eligibility, special constraints…"/></Field></div></section>
      <section className="card-elev rounded-2xl p-6 md:p-7"><Header icon={<Users2 size={19}/>} title="Squad composition" subtitle="Roster guardrails enforced during sale"/><div className="space-y-4"><Field label="Maximum squad size"><input className="input" type="number" required min={1} step={1} value={form.maxSquadSize} onChange={e=>setForm({...form,maxSquadSize:inputNumber(e.target.value)})}/></Field><div className="grid grid-cols-2 gap-4"><Field label="Minimum male"><input className="input" type="number" required min={0} step={1} value={form.minMale} onChange={e=>setForm({...form,minMale:inputNumber(e.target.value)})}/></Field><Field label="Minimum female"><input className="input" type="number" required min={0} step={1} value={form.minFemale} onChange={e=>setForm({...form,minFemale:inputNumber(e.target.value)})}/></Field></div><div className={`rounded-xl border p-4 text-sm ${Number(form.minMale)+Number(form.minFemale)<=Number(form.maxSquadSize)?"border-primary/20 bg-primary/5 text-white/60":"border-danger/40 bg-danger/10 text-danger"}`}><div className="flex items-center gap-2 font-medium"><ShieldCheck size={16}/>Roster validation</div><p className="mt-2 text-xs leading-5">Required composition uses {Number(form.minMale) + Number(form.minFemale)} of {form.maxSquadSize} available slots.</p></div><div className="grid grid-cols-2 gap-3"><Metric icon={<BadgeIndianRupee size={15}/>} label="Opening price" value={`₹${Number(form.playerBasePrice).toLocaleString("en-IN")}`}/><Metric icon={<Clock3 size={15}/>} label="Bid window" value={`${form.timerSeconds}s`}/></div></div></section>
      <div className="lg:col-span-2 flex flex-col gap-3 rounded-2xl border border-white/10 bg-white/[.02] p-5 sm:flex-row sm:items-center sm:justify-between"><p className="max-w-2xl text-xs leading-5 text-white/45">Auction and roster rules lock after the first auction action. Amounts are in rupees. Saving also synchronizes Auction Policy and Roster Policy.</p><span role="status" className="text-sm text-danger">{validationError}</span><button className="btn btn-primary px-7 py-3" disabled={!valid || saving}><Save size={16}/>{saving?"Saving…":"Save & activate"}</button></div>
    </form>
  </div></div>;
}

function Header({icon,title,subtitle}:{icon:React.ReactNode;title:string;subtitle:string}) { return <div className="mb-6 flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl border border-primary/20 bg-primary/10 text-primary">{icon}</div><div><h2 className="h-heading text-lg">{title}</h2><p className="mt-1 text-xs text-white/40">{subtitle}</p></div></div>; }
function Field({label,children}:{label:string;children:React.ReactNode}) { return <label className="block"><span className="label-cap mb-2 block">{label}</span>{children}</label>; }
function Money({label,value,onChange}:{label:string;value:number | "";onChange:(value:number | "")=>void}) { return <Field label={label}><input className="input" type="number" required min={0.01} step={0.01} inputMode="decimal" value={value} onChange={e=>onChange(inputNumber(e.target.value))}/></Field>; }
function Metric({icon,label,value}:{icon:React.ReactNode;label:string;value:string}) { return <div className="rounded-xl border border-white/10 bg-white/[.025] p-3"><div className="flex items-center gap-2 text-primary">{icon}<span className="label-cap text-[9px]">{label}</span></div><div className="mt-2 text-sm font-semibold">{value}</div></div>; }
