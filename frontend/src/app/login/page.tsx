"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { toast } from "sonner";
import { ArrowRight, Eye, EyeOff } from "lucide-react";
import { AuthLayout } from "@/components/AuthLayout";
import { Button, Field, IconButton, Input } from "@/components/ui";

export default function LoginPage() {
  const router = useRouter();
  const { login, hydrate, user } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [visible, setVisible] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => { hydrate(); }, [hydrate]);
  useEffect(() => { if (user) router.replace("/dashboard"); }, [user, router]);
  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault(); if (loading || !email || !password) return;
    setLoading(true); setError("");
    try { await login(email, password); toast.success("Welcome to Play-Neml"); router.replace("/dashboard"); }
    catch (err: unknown) { setError((err as { response?: { data?: { message?: string } } }).response?.data?.message || "Could not sign in. Check your email and password and try again."); }
    finally { setLoading(false); }
  }
  return <AuthLayout eyebrow="Your championship workspace" title="Welcome back" description="Sign in to manage your championship and keep the competition moving.">
    <form onSubmit={handleSubmit} className="space-y-5" aria-busy={loading}>
      {error && <p id="login-error" role="alert" className="form-error">{error}</p>}
      <Field label="Email">{props => <Input {...props} data-testid="login-email" type="email" autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com" required />}</Field>
      <Field label="Password">{props => <div className="relative"><Input {...props} data-testid="login-password" type={visible ? "text" : "password"} autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Enter your password" className="pr-14" required /><IconButton label={visible ? "Hide password" : "Show password"} aria-pressed={visible} onClick={() => setVisible(!visible)} className="absolute right-1 top-0.5 border-transparent">{visible ? <EyeOff size={17} /> : <Eye size={17} />}</IconButton></div>}</Field>
      <Button type="submit" data-testid="login-submit" loading={loading} className="w-full" aria-describedby={error ? "login-error" : undefined}>{loading ? "Signing in…" : "Sign in"}{!loading && <ArrowRight size={16} />}</Button>
    </form>
    <div className="mt-8 border-t border-border pt-6"><p className="text-sm font-medium">Here to follow the action?</p><p className="mt-2 text-xs leading-relaxed text-muted">Use your championship room code for spectator access.</p><Button variant="ghost" data-testid="quick-viewer" onClick={() => router.push("/room/join")} className="mt-4 w-full">Join a championship room<ArrowRight size={15} /></Button></div>
    <p className="mt-6 text-xs leading-relaxed text-muted">New staff member? Open the account setup link shared by your administrator.</p>
  </AuthLayout>;
}
