import Link from "next/link";
import { ArrowUpRight, Gavel, ShieldCheck, Trophy } from "lucide-react";
import { Brand } from "./Brand";
import { ThemeToggle } from "./theme-toggle";

export function AuthLayout({ children, eyebrow, title, description }: { children: React.ReactNode; eyebrow: string; title: string; description: string }) {
  return <main className="auth-layout">
    <section className="auth-story" aria-label="About Play-Neml">
      <Link href="/login" aria-label="Play-Neml home"><Brand /></Link>
      <div className="auth-story-content"><p className="label-cap text-primary">The championship starts here</p><h2>One Platform.<br /><span>Every Championship.</span></h2><p className="mt-6 max-w-sm text-base leading-relaxed text-muted">From the first bid to the final point. Bring your people, teams and competition together.</p>
        <svg className="auth-track" viewBox="0 0 520 230" fill="none" aria-hidden="true"><path d="M-90 240 120 30Q150 0 195 0h235q80 0 40 70L340 230M-30 250 160 60q20-20 55-20h200q45 0 20 40L310 240M35 260 196 94q14-14 38-14h156q17 0 5 20L285 250" stroke="currentColor" strokeWidth="1.5" /><path d="m260 122 40 22-40 22z" fill="currentColor" /></svg>
      </div>
      <div className="auth-features"><span><Gavel size={16} />Live auctions</span><span><Trophy size={16} />Multi-sport</span><span><ShieldCheck size={16} />Role-based access</span></div>
    </section>
    <section className="auth-form-area">
      <div className="auth-mobile-brand"><Link href="/login" aria-label="Play-Neml home"><Brand /></Link></div><div className="auth-theme"><ThemeToggle /></div>
      <div className="auth-form-content"><p className="label-cap mb-3">{eyebrow}</p><h1 className="page-title">{title}</h1><p className="mb-8 mt-3 text-sm leading-relaxed text-muted">{description}</p>{children}</div>
      <p className="auth-footer">Play-Neml<span className="mx-2">/</span>Built for the spirit of competition <ArrowUpRight size={12} aria-hidden="true" /></p>
    </section>
  </main>;
}
