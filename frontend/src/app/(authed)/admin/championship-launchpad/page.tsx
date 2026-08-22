"use client";

import Link from "next/link";
import { FormEvent, useMemo, useState } from "react";
import { useAuth } from "@/lib/auth";
import { api, ChampionshipDto } from "@/lib/api";
import { useChampionship } from "@/lib/championship";
import {
  ArrowLeft, ArrowRight, Check, Eye, EyeOff,
  KeyRound, MailCheck, MessageSquareText, Rocket, ShieldCheck,
  Sparkles, Trophy, UserPlus,
} from "lucide-react";
import { toast } from "sonner";

interface CreateResponse {
  championship: ChampionshipDto;
  admin: { userId: string; fullName: string; email: string; mobile: string; temporaryPassword: string; messageQueued: boolean };
}

const initialForm = {
  name: "", sportType: "BADMINTON", roomCode: "", passcode: "", isPublic: false,
  adminFullName: "", adminEmail: "", adminMobile: "",
};

export default function ChampionshipLaunchpadPage() {
  const user = useAuth((state) => state.user);
  const { load, select } = useChampionship();
  const [form, setForm] = useState(initialForm);
  const [showPasscode, setShowPasscode] = useState(false);
  const [loading, setLoading] = useState(false);
  const [created, setCreated] = useState<CreateResponse | null>(null);

  const ready = useMemo(() => Boolean(
    form.name.trim() && form.sportType &&
    form.adminFullName.trim() && form.adminEmail.trim() && form.adminMobile.trim() &&
    (form.isPublic || form.passcode.trim())
  ), [form]);

  if (user?.role !== "SUPER_ADMIN") {
    return <div className="p-10"><h1 className="h-heading text-3xl">Super Admin access required</h1></div>;
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!ready) return toast.error("Complete the required launch details");
    setLoading(true);
    try {
      const { data } = await api.post<CreateResponse>("/championships", form);
      await load();
      select(data.championship);
      setCreated(data);
      toast.success("Championship and administrator provisioned");
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Championship launch failed");
    } finally {
      setLoading(false);
    }
  }

  if (created) {
    return (
      <div className="grid min-h-screen place-items-center p-6 md:p-10">
        <div className="card-elev relative w-full max-w-3xl overflow-hidden rounded-3xl border border-primary/25 p-8 md:p-12">
          <div className="absolute -right-20 -top-24 h-64 w-64 rounded-full bg-primary/10 blur-3xl"/>
          <div className="relative">
            <div className="mb-7 grid h-16 w-16 place-items-center rounded-2xl border border-primary/30 bg-primary/15 text-primary"><Check size={30}/></div>
            <div className="label-cap text-primary">Launch complete</div>
            <h1 className="h-heading mt-2 text-4xl md:text-5xl">{created.championship.name}</h1>
            <p className="mt-3 max-w-xl text-sm leading-6 text-white/50">The championship workspace, administrator access and credential notification were created in one transaction. The administrator can now configure the auction.</p>
            <div className="mt-8 grid gap-4 md:grid-cols-2">
              <Summary label="Room code" value={created.championship.roomCode} icon={<Trophy size={18}/>}/>
              <Summary label="Administrator" value={created.admin.fullName} icon={<ShieldCheck size={18}/>}/>
              <Summary label="Admin email" value={created.admin.email} icon={<MailCheck size={18}/>}/>
              <Summary label="One-time password" value={created.admin.temporaryPassword} icon={<KeyRound size={18}/>}/>
              <Summary label="Message status" value={created.admin.messageQueued ? "Queued" : "Not queued"} icon={<MessageSquareText size={18}/>}/>
            </div>
            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link href="/admin/championships" className="btn btn-primary justify-center py-3">View championships<ArrowRight size={16}/></Link>
              <button className="btn btn-ghost justify-center py-3" onClick={() => { setCreated(null); setForm(initialForm); }}>Launch another</button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen p-5 md:p-10">
      <div className="mx-auto max-w-7xl">
        <Link href="/admin/championships" className="mb-7 inline-flex items-center gap-2 text-xs text-white/45 transition-colors hover:text-white"><ArrowLeft size={14}/>Back to championships</Link>
        <div className="mb-9 grid gap-6 lg:grid-cols-[1fr_360px] lg:items-end">
          <div>
            <div className="label-cap text-primary">Super Admin Launchpad</div>
            <h1 className="h-heading mt-2 max-w-3xl text-4xl font-bold leading-tight md:text-6xl">Launch a championship and its command team.</h1>
            <p className="mt-4 max-w-2xl text-sm leading-6 text-white/50">One guided flow creates the competition, dedicated administrator and outbound credential notification. Competition rules remain with that administrator.</p>
          </div>
          <div className="card-elev rounded-2xl border border-white/10 p-5">
            <div className="flex items-center gap-3"><Sparkles className="text-primary" size={20}/><div><div className="h-heading text-sm">Atomic provisioning</div><div className="mt-1 text-xs text-white/40">Everything succeeds together—or rolls back.</div></div></div>
          </div>
        </div>

        <form onSubmit={submit} className="grid gap-6 xl:grid-cols-2">
          <Section number="01" title="Championship" subtitle="Identity and viewer access" icon={<Trophy size={19}/>}>
            <Field label="Championship name" required><input className="input" value={form.name} onChange={(e) => setForm({...form, name:e.target.value})} placeholder="Corporate Badminton League" required/></Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Sport" required><select className="input" value={form.sportType} onChange={(e) => setForm({...form, sportType:e.target.value})}><option>BADMINTON</option><option>CRICKET</option><option>FOOTBALL</option><option>BASKETBALL</option><option>VOLLEYBALL</option><option value="OTHER">OTHER</option></select></Field>
              <Field label="Room code"><input className="input uppercase" maxLength={32} value={form.roomCode} onChange={(e) => setForm({...form, roomCode:e.target.value.toUpperCase()})} placeholder="Auto-generated"/></Field>
            </div>
            <label className="flex cursor-pointer items-center justify-between rounded-xl border border-white/10 bg-white/[0.025] p-4">
              <div><div className="text-sm font-medium">Public viewer room</div><div className="mt-1 text-xs text-white/40">Anyone with the room code can follow.</div></div>
              <input type="checkbox" className="h-5 w-5 accent-[var(--color-primary)]" checked={form.isPublic} onChange={(e) => setForm({...form, isPublic:e.target.checked, passcode:e.target.checked ? "" : form.passcode})}/>
            </label>
            {!form.isPublic && <Field label="Viewer passcode" required><div className="relative"><input className="input pr-11" type={showPasscode?"text":"password"} value={form.passcode} onChange={(e) => setForm({...form, passcode:e.target.value})} placeholder="Room access passcode" required/><button type="button" aria-label="Toggle passcode visibility" onClick={() => setShowPasscode(!showPasscode)} className="absolute right-3 top-3 text-white/35 hover:text-white">{showPasscode?<EyeOff size={17}/>:<Eye size={17}/>}</button></div></Field>}
          </Section>

          <Section number="02" title="Championship Admin" subtitle="Dedicated operational owner" icon={<UserPlus size={19}/>}>
            <Field label="Full name" required><input className="input" value={form.adminFullName} onChange={(e) => setForm({...form, adminFullName:e.target.value})} placeholder="Championship administrator" required/></Field>
            <Field label="Email address" required><input className="input" type="email" value={form.adminEmail} onChange={(e) => setForm({...form, adminEmail:e.target.value})} placeholder="admin@company.com" required/></Field>
            <Field label="Mobile number" required><input className="input" type="tel" value={form.adminMobile} onChange={(e) => setForm({...form, adminMobile:e.target.value})} placeholder="+91 98765 43210" required/></Field>
            <div className="rounded-xl border border-secondary/20 bg-secondary/5 p-4 text-xs leading-5 text-white/55">
              <div className="mb-2 flex items-center gap-2 font-medium text-secondary"><KeyRound size={15}/>Secure credential delivery</div>
              A strong temporary password is generated server-side, BCrypt-hashed for login, and queued in message_tracker for delivery.
            </div>
          </Section>

          <div className="xl:col-span-2 grid gap-5 rounded-2xl border border-white/10 bg-white/[0.02] p-5 md:grid-cols-[1fr_auto] md:items-center">
            <div>
              <div className="h-heading text-lg">Ready to create the command workspace?</div>
              <div className="mt-2 flex items-start gap-2 text-xs leading-5 text-white/40"><ShieldCheck className="mt-0.5 shrink-0 text-primary" size={15}/>The championship starts in Draft. Its generated administrator configures auction rules, purse and roster composition before activation.</div>
            </div>
            <button disabled={loading || !ready} className="btn btn-primary w-full justify-center py-4 text-sm disabled:cursor-not-allowed disabled:opacity-40">
              {loading ? "Provisioning workspace…" : <><Rocket size={18}/>Launch championship</>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function Section({number,title,subtitle,icon,children}:{number:string;title:string;subtitle:string;icon:React.ReactNode;children:React.ReactNode}) {
  return <section className="card-elev rounded-2xl border border-white/10 p-6 md:p-7"><div className="mb-6 flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl border border-primary/20 bg-primary/10 text-primary">{icon}</div><div className="min-w-0 flex-1"><div className="flex items-center justify-between"><h2 className="h-heading text-lg">{title}</h2><span className="label-cap text-primary">{number}</span></div><p className="mt-1 text-xs text-white/40">{subtitle}</p></div></div><div className="space-y-4">{children}</div></section>;
}

function Field({label,required=false,children}:{label:string;required?:boolean;children:React.ReactNode}) {
  return <label className="block"><span className="label-cap mb-2 block">{label}{required&&<span className="ml-1 text-primary">*</span>}</span>{children}</label>;
}

function Summary({label,value,icon}:{label:string;value:string;icon:React.ReactNode}) {
  return <div className="rounded-2xl border border-white/10 bg-white/[0.025] p-4"><div className="flex items-center gap-2 text-primary">{icon}<span className="label-cap">{label}</span></div><div className="mt-3 break-all text-sm font-medium">{value}</div></div>;
}
