"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Eye } from "lucide-react";
import { api, ChampionshipDto } from "@/lib/api";
import { useChampionship } from "@/lib/championship";
import { toast } from "sonner";
import { AuthLayout } from "@/components/AuthLayout";
import { Button, Field, Input } from "@/components/ui";

export default function JoinRoomPage() {
  const router = useRouter(); const setViewer = useChampionship(state => state.setViewer);
  const [roomCode, setRoomCode] = useState(""); const [passcode, setPasscode] = useState(""); const [loading, setLoading] = useState(false); const [error, setError] = useState("");
  async function submit(event: FormEvent) {
    event.preventDefault(); if (loading) return; setLoading(true); setError("");
    try { const { data } = await api.post<{ token: string; championship: ChampionshipDto }>("/championships/join-room", { roomCode, passcode }); setViewer(data.championship, data.token); toast.success(`Joined ${data.championship.name} in viewer mode`); router.replace("/dashboard"); }
    catch (err: unknown) { setError((err as { response?: { data?: { message?: string } } }).response?.data?.message || "Could not join that room. Check the room code and passcode."); }
    finally { setLoading(false); }
  }
  return <AuthLayout eyebrow="Spectator access" title="Join a championship" description="Follow the auction, teams, fixtures and live scores. Enter the room code shared by your championship administrator.">
    <form onSubmit={submit} className="space-y-5" aria-busy={loading}>
      {error && <p role="alert" className="form-error">{error}</p>}
      <Field label="Room code">{props => <Input {...props} className="uppercase" value={roomCode} onChange={e => setRoomCode(e.target.value)} placeholder="Championship room code" autoComplete="off" required />}</Field>
      <Field label="Passcode" hint="Only required for rooms protected by a passcode.">{props => <Input {...props} type="password" value={passcode} onChange={e => setPasscode(e.target.value)} placeholder="Room passcode" autoComplete="off" />}</Field>
      <Button type="submit" loading={loading} className="w-full"><Eye size={16} />{loading ? "Joining…" : "Enter viewer room"}</Button>
    </form>
    <div className="mt-8 border-t border-border pt-6"><p className="text-xs leading-relaxed text-muted">Viewer rooms provide read-only access. Championship staff use their own account.</p><Link href="/login" className="btn btn-ghost mt-4 w-full">Staff sign in</Link></div>
  </AuthLayout>;
}
