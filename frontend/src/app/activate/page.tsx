"use client";

import { FormEvent, useEffect, useRef, useState, useSyncExternalStore } from "react";
import Link from "next/link";
import { Check, CheckCircle2, KeyRound } from "lucide-react";
import { api } from "@/lib/api";
import { AuthLayout } from "@/components/AuthLayout";
import { Button, Field, Input } from "@/components/ui";

const subscribe = () => () => {};
export default function ActivatePage() {
  const token = useRef("");
  const mounted = useSyncExternalStore(subscribe, () => true, () => false);
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [complete, setComplete] = useState(false);
  const [hasToken, setHasToken] = useState<boolean | null>(null);
  useEffect(() => {
    token.current = new URLSearchParams(window.location.hash.slice(1)).get("token") || token.current;
    window.history.replaceState(null, "", window.location.pathname);
    // State follows the asynchronous hydration boundary; token is never rendered or persisted.
    queueMicrotask(() => setHasToken(Boolean(token.current)));
  }, []);
  async function submit(event: FormEvent) {
    event.preventDefault(); if (busy || !token.current) return;
    if (password !== confirmation) { setMessage("Passwords must match"); return; }
    if (new TextEncoder().encode(password).length > 72) { setMessage("Password must be at most 72 UTF-8 bytes"); return; }
    setBusy(true); setMessage("");
    try { await api.post("/auth/activate", { token: token.current, password }); setComplete(true); token.current = ""; setPassword(""); setConfirmation(""); }
    catch (error: unknown) { setMessage((error as { response?: { data?: { message?: string } } }).response?.data?.message || "Setup failed. Request a new link from your administrator."); }
    finally { setBusy(false); }
  }
  return <AuthLayout eyebrow="Account activation" title={complete ? "You're ready to play" : "Set up your account"} description={complete ? "Your account is active. Sign in to open your championship workspace." : "Choose a password to securely activate your Play-Neml account."}>
    {complete ? <div className="motion-success"><CheckCircle2 size={40} className="mb-6 text-success" /><p className="mb-6 text-sm" role="status">Your password is ready.</p><Link className="btn btn-primary w-full" href="/login">Sign in</Link></div> : hasToken === false ? <div><div role="alert" className="form-error"><KeyRound className="mb-3" size={23} /><h2 className="font-semibold">A setup link is required</h2><p className="mt-2">Open the complete link provided by your administrator. If it has expired or has already been used, request a new link.</p></div><Link className="btn btn-ghost mt-5 w-full" href="/login">Return to sign in</Link></div> : <form onSubmit={submit} className="space-y-5" aria-busy={busy}>
      <Field label="Password" hint="At least 12 characters, up to 72 UTF-8 bytes.">{props => <Input {...props} autoComplete="new-password" type="password" minLength={12} maxLength={72} required value={password} onChange={e => setPassword(e.target.value)} />}</Field>
      <Field label="Confirm password">{props => <Input {...props} autoComplete="new-password" type="password" required value={confirmation} onChange={e => setConfirmation(e.target.value)} />}</Field>
      <ul className="space-y-2 text-xs text-muted" aria-label="Password requirements"><li className="flex items-center gap-2"><Check size={14} className={password.length >= 12 ? "text-success" : ""} />12 or more characters</li><li className="flex items-center gap-2"><Check size={14} className={confirmation && confirmation === password ? "text-success" : ""} />Both passwords match</li></ul>
      {message && <div role="alert" className="form-error"><p>{message}</p><p className="mt-2">Expired, invalid or consumed links require a new setup link from your administrator.</p></div>}
      <Button type="submit" loading={busy} disabled={!mounted || hasToken !== true} className="w-full">{busy ? "Saving…" : "Set password"}</Button>
      <p className="text-xs leading-relaxed text-muted">Setup links expire and can only be used once. Keep yours private.</p>
      <Link href="/login" className="block text-center text-sm text-primary underline underline-offset-4">Return to sign in</Link>
    </form>}
  </AuthLayout>;
}
