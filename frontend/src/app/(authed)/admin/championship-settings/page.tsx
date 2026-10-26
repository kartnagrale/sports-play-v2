"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useChampionship } from "@/lib/championship";
import { toast } from "sonner";
import { BadgeIndianRupee, CheckCircle2, Clock3, Save, Settings2, ShieldCheck, Users2 } from "lucide-react";

interface Settings {
  auctionName: string; maxSquadSize: number; purseLimit: number; minMale: number; minFemale: number;
  playerBasePrice: number; bidIncrement: number; timerSeconds: number; customRules: string;
}

const empty: Settings = { auctionName: "", maxSquadSize: 12, purseLimit: 1000000000, minMale: 9, minFemale: 3, playerBasePrice: 2000000, bidIncrement: 500000, timerSeconds: 30, customRules: "" };

export default function ChampionshipSettingsPage() {
  const user = useAuth((state) => state.user);
  const { active, load } = useChampionship();
  const [form, setForm] = useState<Settings>(empty);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const role = user?.championships.find((item) => item.championshipId === active?.id)?.role;
  const allowed = role === "CHAMPIONSHIP_ADMIN";
  const valid = useMemo(() => form.auctionName.trim() && form.maxSquadSize > 0 && form.minMale + form.minFemale <= form.maxSquadSize && form.purseLimit > 0 && form.playerBasePrice > 0 && form.bidIncrement > 0 && form.timerSeconds >= 5, [form]);

  useEffect(() => {
    if (!active) return;
    setLoading(true);
    api.get<Settings>(`/championships/${active.id}/settings`).then(({ data }) => setForm(data)).catch((error) => toast.error(error?.response?.data?.message || "Unable to load settings")).finally(() => setLoading(false));
  }, [active?.id]);

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!active || !valid) return toast.error("Check roster and auction values");
    setSaving(true);
    try {
      await api.put(`/championships/${active.id}/settings`, form);
      await load();
      toast.success("Championship configured and activated");
    } catch (error: any) { toast.error(error?.response?.data?.message || "Configuration failed"); }
    finally { setSaving(false); }
  }

  if (!allowed) return <div className="p-10"><h1 className="h-heading text-3xl">Championship Admin access required</h1></div>;
  if (!active || loading) return <div className="p-10 label-cap">Loading championship configuration…</div>;

  return <div className="min-h-screen p-5 md:p-10"><div className="mx-auto max-w-6xl">
    <div className="mb-8 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between"><div><div className="label-cap text-primary">Championship command centre</div><h1 className="h-heading mt-2 text-4xl font-bold md:text-5xl">Auction & roster settings</h1><p className="mt-3 max-w-2xl text-sm text-white/50">Configure how <span className="text-white">{active.name}</span> will run. Saving a draft creates its auction and activates the workspace.</p></div><span className={`chip ${active.status === "ACTIVE" ? "chip-primary" : ""}`}><CheckCircle2 size={13}/>{active.status}</span></div>
    <form onSubmit={save} className="grid gap-6 lg:grid-cols-[1.1fr_.9fr]">
      <section className="card-elev rounded-2xl p-6 md:p-7"><Header icon={<Settings2 size={19}/>} title="Auction controls" subtitle="Commercial rules used for every bid"/><div className="space-y-4"><Field label="Auction name"><input className="input" required value={form.auctionName} onChange={e=>setForm({...form,auctionName:e.target.value})}/></Field><div className="grid gap-4 sm:grid-cols-2"><Money label="Team purse" value={form.purseLimit} onChange={purseLimit=>setForm({...form,purseLimit})}/><Money label="Player base price" value={form.playerBasePrice} onChange={playerBasePrice=>setForm({...form,playerBasePrice})}/><Money label="Bid increment" value={form.bidIncrement} onChange={bidIncrement=>setForm({...form,bidIncrement})}/><Field label="Bid timer (seconds)"><input className="input" type="number" min={5} value={form.timerSeconds} onChange={e=>setForm({...form,timerSeconds:Number(e.target.value)})}/></Field></div><Field label="Additional rules"><textarea className="input min-h-28 resize-y" value={form.customRules || ""} onChange={e=>setForm({...form,customRules:e.target.value})} placeholder="Tie-breaks, eligibility, special constraints…"/></Field></div></section>
      <section className="card-elev rounded-2xl p-6 md:p-7"><Header icon={<Users2 size={19}/>} title="Squad composition" subtitle="Roster guardrails enforced during sale"/><div className="space-y-4"><Field label="Maximum squad size"><input className="input" type="number" min={1} value={form.maxSquadSize} onChange={e=>setForm({...form,maxSquadSize:Number(e.target.value)})}/></Field><div className="grid grid-cols-2 gap-4"><Field label="Minimum male"><input className="input" type="number" min={0} value={form.minMale} onChange={e=>setForm({...form,minMale:Number(e.target.value)})}/></Field><Field label="Minimum female"><input className="input" type="number" min={0} value={form.minFemale} onChange={e=>setForm({...form,minFemale:Number(e.target.value)})}/></Field></div><div className={`rounded-xl border p-4 text-sm ${form.minMale+form.minFemale<=form.maxSquadSize?"border-primary/20 bg-primary/5 text-white/60":"border-danger/40 bg-danger/10 text-danger"}`}><div className="flex items-center gap-2 font-medium"><ShieldCheck size={16}/>Roster validation</div><p className="mt-2 text-xs leading-5">Required composition uses {form.minMale + form.minFemale} of {form.maxSquadSize} available slots.</p></div><div className="grid grid-cols-2 gap-3"><Metric icon={<BadgeIndianRupee size={15}/>} label="Opening price" value={`₹${Number(form.playerBasePrice).toLocaleString("en-IN")}`}/><Metric icon={<Clock3 size={15}/>} label="Bid window" value={`${form.timerSeconds}s`}/></div></div></section>
      <div className="lg:col-span-2 flex flex-col gap-3 rounded-2xl border border-white/10 bg-white/[.02] p-5 sm:flex-row sm:items-center sm:justify-between"><p className="max-w-2xl text-xs leading-5 text-white/45">Purse changes are blocked after bidding begins, protecting financial consistency. Other rules should also be finalized before the live auction.</p><button className="btn btn-primary px-7 py-3" disabled={!valid || saving}><Save size={16}/>{saving?"Saving…":"Save & activate"}</button></div>
    </form>
  </div></div>;
}

function Header({icon,title,subtitle}:{icon:React.ReactNode;title:string;subtitle:string}) { return <div className="mb-6 flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl border border-primary/20 bg-primary/10 text-primary">{icon}</div><div><h2 className="h-heading text-lg">{title}</h2><p className="mt-1 text-xs text-white/40">{subtitle}</p></div></div>; }
function Field({label,children}:{label:string;children:React.ReactNode}) { return <label className="block"><span className="label-cap mb-2 block">{label}</span>{children}</label>; }
function Money({label,value,onChange}:{label:string;value:number;onChange:(value:number)=>void}) { return <Field label={label}><input className="input" type="number" min={1} step={100000} value={value} onChange={e=>onChange(Number(e.target.value))}/></Field>; }
function Metric({icon,label,value}:{icon:React.ReactNode;label:string;value:string}) { return <div className="rounded-xl border border-white/10 bg-white/[.025] p-3"><div className="flex items-center gap-2 text-primary">{icon}<span className="label-cap text-[9px]">{label}</span></div><div className="mt-2 text-sm font-semibold">{value}</div></div>; }
