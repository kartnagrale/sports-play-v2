"use client";

import { useEffect, useRef, useState } from "react";
import { Gavel, ShieldCheck, Trophy, UsersRound } from "lucide-react";

const steps = [
  { label: "Secure access", icon: ShieldCheck },
  { label: "Live auction", icon: Gavel },
  { label: "Team rosters", icon: UsersRound },
];

export default function SplashScreen({ onComplete }: { onComplete: () => void }) {
  const [leaving, setLeaving] = useState(false);
  const onCompleteRef = useRef(onComplete);
  onCompleteRef.current = onComplete;

  useEffect(() => {
    const finish = window.setTimeout(() => setLeaving(true), 650);
    const remove = window.setTimeout(() => onCompleteRef.current(), 950);
    return () => { window.clearTimeout(finish); window.clearTimeout(remove); };
  }, []);

  return (
    <div className={`fixed inset-0 z-50 grid place-items-center overflow-hidden bg-bg transition-opacity duration-300 ${leaving ? "pointer-events-none opacity-0" : "opacity-100"}`}>
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_38%,color-mix(in_srgb,var(--color-primary)_14%,transparent),transparent_42%)]" />
      <div className="relative w-full max-w-xl px-6 text-center">
        <div className="mx-auto grid h-16 w-16 place-items-center rounded-2xl border border-primary/30 bg-primary/10 text-primary shadow-[0_0_45px_color-mix(in_srgb,var(--color-primary)_25%,transparent)]">
          <Trophy size={29} />
        </div>
        <div className="h-heading mt-6 text-5xl font-bold tracking-tight md:text-6xl">
          Play<span className="text-primary">NeML</span>
        </div>
        <div className="label-cap mt-3 text-[10px] text-white/45">Multi-championship sports platform</div>
        <div className="mt-8 flex justify-center gap-3">
          {steps.map(({ label, icon: Icon }) => (
            <div key={label} className="flex items-center gap-2 rounded-full border border-white/10 bg-white/[.035] px-3 py-2 text-[10px] text-white/55">
              <Icon size={13} className="text-primary" />{label}
            </div>
          ))}
        </div>
        <div className="mx-auto mt-8 h-1 w-52 overflow-hidden rounded-full bg-white/10">
          <div className="loading-bar h-full bg-gradient-to-r from-primary to-secondary" />
        </div>
        <div className="label-cap mt-4 text-[9px] text-white/35">Preparing your workspace</div>
      </div>
    </div>
  );
}
