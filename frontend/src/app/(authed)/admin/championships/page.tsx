"use client";

import Link from "next/link";
import { useEffect } from "react";
import { useAuth } from "@/lib/auth";
import { useChampionship } from "@/lib/championship";
import { ArrowRight, Radio, Rocket, Shield } from "lucide-react";

export default function ChampionshipsAdminPage() {
  const user = useAuth((state) => state.user);
  const { championships, load, select } = useChampionship();

  useEffect(() => { load(); }, [load]);

  if (user?.role !== "SUPER_ADMIN") {
    return <div className="p-10"><h1 className="h-heading text-3xl">Super Admin access required</h1></div>;
  }

  return (
    <div className="min-h-screen p-6 md:p-10">
      <div className="mx-auto max-w-7xl">
        <div className="mb-8 flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
          <div>
            <div className="label-cap">Platform portfolio</div>
            <h1 className="h-heading mt-2 text-4xl font-bold md:text-5xl">Championships</h1>
            <p className="mt-3 max-w-2xl text-sm text-white/50">Monitor every competition and move between championship workspaces.</p>
          </div>
          <Link href="/admin/championship-launchpad" className="btn btn-primary justify-center py-3">
            <Rocket size={16}/>Launch championship<ArrowRight size={15}/>
          </Link>
        </div>

        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {championships.map((championship) => (
            <button key={championship.id} onClick={() => { select(championship); window.location.href = "/dashboard"; }}
              className="card-elev group rounded-2xl border border-white/10 p-6 text-left transition-all duration-300 hover:-translate-y-1 hover:border-primary/40">
              <div className="flex items-center justify-between">
                <div className="grid h-11 w-11 place-items-center rounded-xl border border-primary/20 bg-primary/10"><Shield className="text-primary" size={20}/></div>
                <span className="chip"><Radio size={11}/>{championship.status}</span>
              </div>
              <h2 className="h-heading mt-6 text-2xl">{championship.name}</h2>
              <div className="mt-1 text-sm text-white/45">{championship.sportType}</div>
              <div className="mt-6 flex items-end justify-between border-t border-white/10 pt-4">
                <div><div className="label-cap">Room code</div><div className="stat-num mt-1 text-xl text-primary">{championship.roomCode}</div></div>
                <ArrowRight className="text-white/25 transition-transform group-hover:translate-x-1 group-hover:text-primary" size={18}/>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
