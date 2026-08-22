"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "@/lib/auth";
import { useChampionship, VIEWER_TOKEN_KEY } from "@/lib/championship";
import { useNavigation } from "@/lib/navigation";
import type { LucideIcon } from "lucide-react";
import {
  LayoutDashboard, Gavel, Trophy, CalendarDays, UsersRound, UserRound,
  ChartNoAxesCombined, Medal, Sparkles, History, Bell, LogOut,
  ShieldCheck, Menu, X, ChevronRight, Circle, Radio,
} from "lucide-react";

const ICONS: Record<string, LucideIcon> = {
  LayoutDashboard, Gavel, Trophy, CalendarDays, UsersRound, UserRound,
  ChartNoAxesCombined, Medal, Sparkles, History, Bell, ShieldCheck,
};

const ROLE_LABELS = {
  SUPER_ADMIN: "Platform administrator",
  CHAMPIONSHIP_ADMIN: "Championship administrator",
  TEAM_CAPTAIN: "Team captain",
  SPECTATOR: "Spectator",
};

export default function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const { user, hydrate, hydrated, logout } = useAuth();
  const { championships, active, hydrated: championshipHydrated, load, select } = useChampionship();
  const navigation = useNavigation();

  useEffect(() => { hydrate(); }, [hydrate]);
  useEffect(() => { if (hydrated) load(); }, [hydrated, load, user]);

  useEffect(() => {
    if (!hydrated || !championshipHydrated) return;
    const isViewer = typeof window !== "undefined" && Boolean(localStorage.getItem(VIEWER_TOKEN_KEY));
    if (!user && !isViewer) {
      router.replace("/login");
      return;
    }
    navigation.load(active?.id);
  }, [active?.id, championshipHydrated, hydrated, user, router, navigation.load]);

  const sections = useMemo(() => {
    const grouped = new Map<string, typeof navigation.screens>();
    navigation.screens.forEach((screen) => {
      grouped.set(screen.section, [...(grouped.get(screen.section) || []), screen]);
    });
    return [...grouped.entries()];
  }, [navigation.screens]);

  const allowed = navigation.screens.some(
    (screen) => pathname === screen.path || pathname.startsWith(screen.path + "/")
  );

  useEffect(() => {
    if (!navigation.loading && navigation.screens.length > 0 && !allowed) {
      router.replace(navigation.screens[0].path);
    }
  }, [allowed, navigation.loading, navigation.screens, router]);

  const handleLogout = async () => {
    navigation.clear();
    await logout();
    router.replace("/login");
  };

  const changeChampionship = (id: string) => {
    const next = championships.find((championship) => championship.id === id);
    if (!next) return;
    select(next);
    setMenuOpen(false);
    router.push("/dashboard");
  };

  if (!hydrated || !championshipHydrated) return <ShellLoading />;

  return (
    <div className="min-h-screen bg-shell lg:flex">
      {menuOpen && (
        <button className="fixed inset-0 z-30 bg-black/65 backdrop-blur-sm lg:hidden"
          aria-label="Close navigation" onClick={() => setMenuOpen(false)} />
      )}

      <aside className={`fixed inset-y-0 left-0 z-40 flex w-[286px] flex-col border-r border-white/10 bg-bg-surface/95 shadow-2xl backdrop-blur-2xl transition-transform duration-300 ease-out lg:sticky lg:top-0 lg:h-screen lg:translate-x-0 lg:shadow-none ${menuOpen ? "translate-x-0" : "-translate-x-full"}`}>
        <div className="border-b border-white/10 px-5 pb-5 pt-6">
          <div className="flex items-start justify-between">
            <div>
              <div className="text-primary h-heading text-2xl font-bold leading-none drop-shadow-glow" data-testid="brand-name">PlayNeml</div>
              <div className="label-cap mt-1">Khelo Corporate</div>
            </div>
            <button onClick={() => setMenuOpen(false)} className="nav-icon-button lg:hidden" aria-label="Close menu"><X size={18}/></button>
          </div>
          {championships.length > 0 && (
            <div className="relative mt-5">
              <div className="label-cap mb-2 text-[9px]">Active championship</div>
              <select aria-label="Active championship" value={active?.id || ""} onChange={(event) => changeChampionship(event.target.value)}
                className="input appearance-none py-2.5 pr-9 text-xs font-medium">
                {championships.map((championship) => <option key={championship.id} value={championship.id}>{championship.name} · {championship.sportType}</option>)}
              </select>
              <ChevronRight size={14} className="pointer-events-none absolute bottom-3 right-3 rotate-90 text-white/35"/>
            </div>
          )}
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-4" aria-label="Role-based navigation">
          {navigation.loading ? <NavSkeleton /> : sections.map(([section, screens]) => (
            <div key={section} className="mb-5 last:mb-0">
              <div className="label-cap mb-2 px-3 text-[9px]">{section}</div>
              <div className="space-y-1">
                {screens.map((screen) => {
                  const Icon = ICONS[screen.icon] || Circle;
                  const isActive = pathname === screen.path || pathname.startsWith(screen.path + "/");
                  return (
                    <Link key={screen.code} href={screen.path} onClick={() => setMenuOpen(false)} data-testid={`nav-${screen.code.toLowerCase()}`}
                      className={`group relative flex items-center gap-3 rounded-xl border px-3 py-2.5 h-heading text-xs uppercase tracking-widest transition-all duration-200 ${isActive ? "border-primary/25 bg-primary/10 text-primary shadow-[inset_3px_0_0_var(--color-primary)]" : "border-transparent text-white/55 hover:translate-x-0.5 hover:border-white/10 hover:bg-white/[0.045] hover:text-white"}`}>
                      <Icon size={17} strokeWidth={2.1}/><span className="flex-1">{screen.label}</span>
                      <ChevronRight size={13} className={`transition-all ${isActive ? "opacity-70" : "-translate-x-1 opacity-0 group-hover:translate-x-0 group-hover:opacity-50"}`}/>
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        <div className="border-t border-white/10 p-4">
          <div className="mb-3 flex items-center gap-3 rounded-xl bg-white/[0.035] p-3">
            <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl border border-primary/30 bg-primary/10 h-heading font-bold text-primary">
              {user?.fullName?.charAt(0) || <Radio size={16}/>}
            </div>
            <div className="min-w-0 flex-1">
              <div className="truncate text-sm font-medium" data-testid="user-name">{user?.fullName || "Room viewer"}</div>
              <div className="mt-0.5 truncate text-[9px] label-cap">{navigation.role ? ROLE_LABELS[navigation.role] : "Loading access"}</div>
            </div>
          </div>
          <button onClick={handleLogout} className="btn btn-ghost w-full justify-center text-xs" data-testid="logout-btn"><LogOut size={14}/>{user ? "Logout" : "Leave room"}</button>
        </div>
      </aside>

      <div className="min-w-0 flex-1">
        <header className="sticky top-0 z-20 flex h-16 items-center border-b border-white/10 bg-bg/80 px-4 backdrop-blur-xl lg:hidden">
          <button onClick={() => setMenuOpen(true)} className="nav-icon-button" aria-label="Open navigation"><Menu size={19}/></button>
          <div className="ml-3 min-w-0"><div className="h-heading text-sm font-semibold">{active?.name || "PlayNeml"}</div><div className="label-cap truncate text-[8px]">{navigation.role ? ROLE_LABELS[navigation.role] : "Loading"}</div></div>
        </header>
        {navigation.loading ? <ContentLoading /> : navigation.screens.length === 0 ? <NoAccess /> : allowed ? (
          <main key={active?.id || "global"} className="page-enter min-h-screen min-w-0 overflow-x-hidden">{children}</main>
        ) : <ContentLoading />}
      </div>
    </div>
  );
}

function ShellLoading() {
  return <div className="min-h-screen bg-shell p-6"><div className="mx-auto flex min-h-[80vh] max-w-md flex-col items-center justify-center"><div className="brand-loader mb-5">P</div><div className="h-heading text-lg tracking-[0.3em]">Preparing your workspace</div><div className="mt-4 h-1 w-40 overflow-hidden rounded-full bg-white/10"><div className="loading-bar h-full bg-primary"/></div></div></div>;
}

function NavSkeleton() {
  return <div className="space-y-3 px-2">{Array.from({length: 7}).map((_, i) => <div key={i} className="skeleton h-10 rounded-xl" style={{opacity: 1 - i * .08}} />)}</div>;
}

function ContentLoading() {
  return <main className="min-h-screen p-6 md:p-10"><div className="skeleton h-4 w-24 rounded"/><div className="skeleton mt-4 h-12 w-full max-w-lg rounded-xl"/><div className="mt-10 grid gap-5 md:grid-cols-2 xl:grid-cols-4">{Array.from({length: 4}).map((_, i) => <div key={i} className="skeleton h-36 rounded-2xl"/>)}</div></main>;
}

function NoAccess() {
  return <main className="grid min-h-screen place-items-center p-8 text-center"><div><ShieldCheck className="mx-auto mb-5 text-primary" size={42}/><div className="label-cap">Access configuration</div><h1 className="h-heading mt-2 text-3xl">No screens assigned</h1><p className="mt-3 max-w-md text-sm text-white/50">This role has no active screen mappings. Ask a platform administrator to update its database permissions.</p></div></main>;
}
