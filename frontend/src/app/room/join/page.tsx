"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { DoorOpen, Eye, LockKeyhole } from "lucide-react";
import { api, ChampionshipDto } from "@/lib/api";
import { useChampionship } from "@/lib/championship";
import { toast } from "sonner";

export default function JoinRoomPage() {
  const router = useRouter(); const setViewer = useChampionship((s) => s.setViewer);
  const [roomCode,setRoomCode]=useState(""); const [passcode,setPasscode]=useState(""); const [loading,setLoading]=useState(false);
  async function submit(e:FormEvent){e.preventDefault();setLoading(true);try{
    const {data}=await api.post<{token:string;championship:ChampionshipDto}>("/championships/join-room",{roomCode,passcode});
    setViewer(data.championship,data.token);toast.success(`Joined ${data.championship.name} in viewer mode`);router.replace("/dashboard");
  }catch(err:any){toast.error(err?.response?.data?.message||"Could not join that room");}finally{setLoading(false);}}
  return <main className="min-h-screen grid place-items-center p-6 bg-bg">
    <section className="card-elev rounded-3xl p-8 w-full max-w-md border border-white/10">
      <div className="w-12 h-12 rounded-xl bg-primary/10 text-primary grid place-items-center mb-6"><DoorOpen /></div>
      <div className="label-cap">Read-only access</div><h1 className="h-heading text-4xl mt-2">Join a championship</h1>
      <p className="text-white/50 text-sm mt-3">Follow the auction, teams, fixtures, and live scores without an account.</p>
      <form onSubmit={submit} className="space-y-4 mt-8">
        <label className="block"><span className="label-cap block mb-2">Room code</span><input className="input uppercase" value={roomCode} onChange={e=>setRoomCode(e.target.value)} placeholder="CRIC-8821" required /></label>
        <label className="block"><span className="label-cap block mb-2">Passcode</span><div className="relative"><LockKeyhole className="absolute left-3 top-3 text-white/30" size={16}/><input className="input pl-10" type="password" value={passcode} onChange={e=>setPasscode(e.target.value)} placeholder="Room passcode" /></div></label>
        <button className="btn btn-primary w-full justify-center py-3" disabled={loading}><Eye size={16}/>{loading?"Joining…":"Enter viewer room"}</button>
      </form>
      <Link href="/login" className="block text-center text-xs text-white/50 hover:text-primary mt-6">Championship staff? Sign in</Link>
    </section>
  </main>;
}
