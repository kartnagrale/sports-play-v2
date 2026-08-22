"use client";

import { FormEvent, useEffect, useState } from "react";
import { api, ChampionshipDto } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useChampionship } from "@/lib/championship";
import { Plus, Radio, Shield } from "lucide-react";
import { toast } from "sonner";

export default function ChampionshipsAdminPage(){
  const user=useAuth(s=>s.user);const {championships,load,select}=useChampionship();
  const [open,setOpen]=useState(false);const [loading,setLoading]=useState(false);
  const [form,setForm]=useState({name:"",sportType:"CRICKET",passcode:"",isPublic:false,maxSquadSize:12,purseLimit:1000000000});
  useEffect(()=>{load();},[load]);
  if(user?.role!=="SUPER_ADMIN")return <div className="p-10"><h1 className="h-heading text-3xl">Super Admin access required</h1></div>;
  async function submit(e:FormEvent){e.preventDefault();setLoading(true);try{const {data}=await api.post<ChampionshipDto>("/championships",form);await load();select(data);setOpen(false);toast.success(`Created ${data.name} · ${data.roomCode}`);}catch(err:any){toast.error(err?.response?.data?.message||"Creation failed");}finally{setLoading(false);}}
  return <div className="p-10"><div className="flex justify-between items-end mb-8"><div><div className="label-cap">SaaS control plane</div><h1 className="h-heading text-5xl mt-1">Championships</h1></div><button className="btn btn-primary" onClick={()=>setOpen(!open)}><Plus size={16}/>New championship</button></div>
    {open&&<form onSubmit={submit} className="card-elev rounded-2xl p-6 grid md:grid-cols-2 gap-4 mb-8">
      <label><span className="label-cap block mb-2">Name</span><input className="input" required value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></label>
      <label><span className="label-cap block mb-2">Sport</span><select className="input" value={form.sportType} onChange={e=>setForm({...form,sportType:e.target.value})}><option>CRICKET</option><option>FOOTBALL</option><option>BADMINTON</option><option>BASKETBALL</option><option>VOLLEYBALL</option><option value="OTHER">OTHER</option></select></label>
      <label><span className="label-cap block mb-2">Room passcode</span><input className="input" type="password" value={form.passcode} onChange={e=>setForm({...form,passcode:e.target.value})}/></label>
      <label><span className="label-cap block mb-2">Squad limit</span><input className="input" type="number" min="1" value={form.maxSquadSize} onChange={e=>setForm({...form,maxSquadSize:Number(e.target.value)})}/></label>
      <label className="flex items-center gap-3"><input type="checkbox" checked={form.isPublic} onChange={e=>setForm({...form,isPublic:e.target.checked})}/><span className="text-sm">Public room (no passcode)</span></label>
      <button className="btn btn-primary justify-center" disabled={loading}>{loading?"Creating…":"Create and generate room"}</button>
    </form>}
    <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-5">{championships.map(c=><button key={c.id} onClick={()=>{select(c);window.location.href="/dashboard";}} className="card-elev rounded-2xl p-6 text-left border border-white/10 hover:border-primary/40"><div className="flex justify-between"><Shield className="text-primary"/><span className="chip"><Radio size={11}/>{c.status}</span></div><h2 className="h-heading text-2xl mt-5">{c.name}</h2><div className="text-white/50 text-sm mt-1">{c.sportType}</div><div className="label-cap mt-5">Room code</div><div className="stat-num text-xl text-primary">{c.roomCode}</div></button>)}</div>
  </div>;
}
