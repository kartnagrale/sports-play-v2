"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "@/lib/auth";
import { useChampionship, VIEWER_TOKEN_KEY } from "@/lib/championship";
import { useNavigation } from "@/lib/navigation";
import type { LucideIcon } from "lucide-react";
import { LayoutDashboard, Gavel, Trophy, CalendarDays, UsersRound, UserRound, ChartNoAxesCombined, Medal, Sparkles, History, Bell, LogOut, ShieldCheck, Menu, ChevronRight, ChevronLeft, Circle, Rocket, Settings2, UserCog, ClipboardCheck, GitBranch } from "lucide-react";
import { Brand } from "./Brand";
import { ThemeToggle } from "./theme-toggle";
import { Button, EmptyState, IconButton, LoadingIndicator, Select, Skeleton, StatusBadge } from "./ui";
import { Dialog } from "./ui/Dialog";
import { DropdownMenu } from "./ui/Disclosure";
import { PlayerAvatar } from "./players/PlayerAvatar";

const ICONS: Record<string, LucideIcon> = { LayoutDashboard, Gavel, Trophy, CalendarDays, UsersRound, UserRound, ChartNoAxesCombined, Medal, Sparkles, History, Bell, ShieldCheck, Rocket, Settings2, UserCog, ClipboardCheck, Brackets: GitBranch };
const ROLE_LABELS = { SUPER_ADMIN: "Platform administrator", CHAMPIONSHIP_ADMIN: "Championship administrator", TEAM_CAPTAIN: "Team captain", SPECTATOR: "Spectator" };

export default function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const { user, hydrate, hydrated, logout } = useAuth();
  const { championships, active, hydrated: championshipHydrated, load, select } = useChampionship();
  const { role: navigationRole, screens, loading: navigationLoading, load: loadNavigation, clear: clearNavigation } = useNavigation();
  useEffect(() => { hydrate(); }, [hydrate]);
  useEffect(() => { if (hydrated) load(); }, [hydrated, load, user]);
  useEffect(() => {
    if (!hydrated || !championshipHydrated) return;
    const isViewer = Boolean(sessionStorage.getItem(VIEWER_TOKEN_KEY));
    if (!user && !isViewer) { router.replace("/login"); return; }
    loadNavigation(active?.id);
  }, [active?.id, championshipHydrated, hydrated, user, router, loadNavigation]);
  // Close the modal drawer when crossing to desktop; never leave an invisible focus trap.
  useEffect(() => {
    const media = window.matchMedia("(min-width: 1024px)");
    const close = () => { if (media.matches) setMenuOpen(false); };
    media.addEventListener("change", close); return () => media.removeEventListener("change", close);
  }, []);
  const sections = useMemo(() => {
    const grouped = new Map<string, typeof screens>();
    screens.forEach(screen => grouped.set(screen.section, [...(grouped.get(screen.section) || []), screen]));
    return [...grouped.entries()];
  }, [screens]);
  const home = (navigationRole === "SUPER_ADMIN" ? screens.find(screen=>screen.path==="/admin/championships")?.path : null) || screens[0]?.path || "/dashboard";
  const allowed = screens.some(screen => pathname === screen.path || pathname.startsWith(screen.path + "/"));
  const currentPage = screens.find(screen => pathname === screen.path || pathname.startsWith(screen.path + "/"))?.label || "Workspace";
  useEffect(() => { document.title = `${currentPage} | Play-Neml`; }, [currentPage]);
  const roleLabel = navigationRole ? ROLE_LABELS[navigationRole] : "Loading access";
  useEffect(() => { if (!navigationLoading && screens.length > 0 && !allowed) router.replace(home); }, [allowed, navigationLoading, screens, router, home]);
  const handleLogout = async () => { clearNavigation(); await logout(); router.replace("/login"); };
  const changeChampionship = (id: string) => {
    const next = championships.find(championship => championship.id === id);
    if (!next) return;
    select(next); setMenuOpen(false); router.push("/dashboard");
  };

  function sidebar(compact: boolean, mobile = false) {
    return <>
      <div className="sidebar-brand"><Link href={home} aria-label="Play-Neml home"><Brand compact={compact} /></Link>{!compact && <p className="mt-3 text-xs text-muted">Championship workspace</p>}</div>
      {!compact && <div className="px-4 pb-4">{championships.length > 0 ? <><label htmlFor={mobile ? "mobile-championship" : "desktop-championship"} className="label-cap mb-2 block">Active championship</label><Select id={mobile ? "mobile-championship" : "desktop-championship"} aria-label="Active championship" value={active?.id || ""} onChange={event => changeChampionship(event.target.value)} className="text-xs">{championships.map(championship => <option key={championship.id} value={championship.id}>{championship.name} · {championship.sportType}</option>)}</Select>{active && <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted"><Trophy size={12} aria-hidden="true" />{active.sportType.replaceAll("_", " ")}<StatusBadge tone={active.status === "ACTIVE" ? "success" : "neutral"}>{active.status}</StatusBadge></div>}</> : <p className="rounded-lg border border-border p-3 text-xs text-muted">Platform workspace</p>}</div>}
      <nav className="flex-1 overflow-y-auto px-3 py-2" aria-label="Role-based navigation">{navigationLoading ? <NavSkeleton /> : sections.map(([section, items]) => <div key={section} className="mb-6 last:mb-0">{!compact && <p className="label-cap mb-2 px-3">{section}</p>}<div className="space-y-1">{items.map(screen => {
        const Icon = ICONS[screen.icon] || Circle;
        const isActive = pathname === screen.path || pathname.startsWith(screen.path + "/");
        return <Link key={screen.code} href={screen.path} aria-current={isActive ? "page" : undefined} aria-label={compact ? screen.label : undefined} title={compact ? screen.label : undefined} onClick={() => setMenuOpen(false)} data-testid={`nav-${screen.code.toLowerCase()}`} className={`sidebar-link ${isActive ? "is-active" : ""} ${compact ? "justify-center px-0" : ""}`}><Icon size={18} aria-hidden="true" />{!compact && <><span className="flex-1">{screen.label}</span>{isActive && <ChevronRight size={14} aria-hidden="true" />}</>}</Link>;
      })}</div></div>)}</nav>
      <div className="border-t border-border p-3">{!compact && <div className="mb-3 flex items-center gap-3 p-2"><PlayerAvatar name={user?.fullName || "Viewer"} size="sm" alt="" /><div className="min-w-0"><p className="truncate text-sm font-medium" data-testid="user-name">{user?.fullName || "Room viewer"}</p><p className="mt-0.5 text-xs text-muted">{roleLabel}</p></div></div>}<Button variant="ghost" onClick={handleLogout} className="w-full" aria-label={user ? "Sign out" : "Leave room"} data-testid="logout-btn"><LogOut size={16} aria-hidden="true" />{!compact && (user ? "Sign out" : "Leave room")}</Button></div>
    </>;
  }
  if (!hydrated || !championshipHydrated) return <div className="grid min-h-screen place-items-center"><LoadingIndicator label="Opening your workspace…" /></div>;
  return <div className="app-shell" data-collapsed={collapsed}>
    <a href="#main-content" className="skip-link">Skip to content</a>
    <aside className="desktop-sidebar">{sidebar(collapsed)}<div className="border-t border-border p-3"><Button variant="ghost" onClick={() => setCollapsed(!collapsed)} aria-label={collapsed ? "Expand navigation" : "Collapse navigation"} aria-expanded={!collapsed} className="w-full">{collapsed ? <ChevronRight size={16} /> : <><ChevronLeft size={16} />Collapse navigation</>}</Button></div></aside>
    {menuOpen && <Dialog title="Navigation" onClose={() => setMenuOpen(false)} className="mobile-drawer">{sidebar(false, true)}</Dialog>}
    <div className="min-w-0 flex-1">
      <header className="workspace-header">
        <IconButton label="Open navigation" onClick={() => setMenuOpen(true)} className="lg:hidden" aria-haspopup="dialog" aria-expanded={menuOpen}><Menu size={19} /></IconButton>
        <div className="min-w-0 flex-1"><p className="header-breadcrumb"><span className="hidden sm:inline">Play-Neml <span className="mx-2 text-muted">/</span></span>{currentPage}</p><p className="mt-1 truncate text-xs text-muted">{active?.name || "Platform workspace"}<span className="mx-2">·</span>{roleLabel}</p></div>
        <div className="hidden xl:block">{active && <StatusBadge tone={active.status === "ACTIVE" ? "success" : "neutral"}>{active.sportType} · {active.status}</StatusBadge>}</div>
        <ThemeToggle />
        <DropdownMenu label="Account menu" trigger={<PlayerAvatar name={user?.fullName || "Viewer"} size="sm" alt="" />} items={[{ label: "Account details", onSelect: () => setAccountOpen(true) }, { label: user ? "Sign out" : "Leave room", onSelect: handleLogout }]} />
      </header>
      <main id="main-content" tabIndex={-1} key={active?.id || "global"} className="shell-main page-enter">
        {navigationLoading ? <ContentLoading /> : screens.length === 0 ? <EmptyState title="No screens assigned" description="Ask your platform administrator to review access for this role." icon={<ShieldCheck size={28} />} /> : allowed ? children : <ContentLoading />}
      </main>
    </div>
    {accountOpen && <Dialog title="Account details" onClose={() => setAccountOpen(false)}><div className="space-y-4 text-sm"><div><p className="label-cap">Name</p><p className="mt-1">{user?.fullName || "Room viewer"}</p></div>{user?.email && <div><p className="label-cap">Email</p><p className="mt-1 break-all">{user.email}</p></div>}<div><p className="label-cap">Portal</p><p className="mt-1">{roleLabel}</p></div>{active && <div><p className="label-cap">Championship</p><p className="mt-1">{active.name}</p></div>}</div></Dialog>}
  </div>;
}
function NavSkeleton() { return <div className="space-y-3 p-2" role="status" aria-label="Loading navigation">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-10" />)}</div>; }
function ContentLoading() { return <div className="p-5 sm:p-10" role="status" aria-label="Loading workspace"><Skeleton className="mb-8 h-9 w-64 max-w-full" /><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-32" />)}</div><Skeleton className="mt-8 h-80" /></div>; }
