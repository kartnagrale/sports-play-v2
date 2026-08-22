"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { toast } from "sonner";
import SplashScreen from "@/components/SplashScreen";

export default function LoginPage() {
  const router = useRouter();
  const { login, hydrate, user } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [showSplash, setShowSplash] = useState(true);

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  useEffect(() => {
    if (user) router.replace("/dashboard");
  }, [user, router]);
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) return;
    setLoading(true);
    try {
      await login(email, password);
      toast.success("Welcome to PlayNeML");
      router.replace("/dashboard");
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Invalid credentials");
    } finally {
      setLoading(false);
    }
  };

  const quickFill = (role: "admin" | "owner" | "viewer") => {
    if (role === "admin") {
      setEmail("admin@neml.com");
      setPassword("");
      toast.info("Enter the super administrator password");
    } else if (role === "owner") {
      setEmail("owner-blr@neml.com");
      setPassword("");
      toast.info("Enter the team owner's database password");
    } else {
      router.push("/room/join");
    }
  };

  return (
    <>
      {showSplash && <SplashScreen onComplete={() => setShowSplash(false)} />}
      <div
        className={`min-h-screen w-full grid lg:grid-cols-2 relative bg-bg transition-opacity duration-1000 ${
          showSplash ? "opacity-0" : "opacity-100"
        }`}
      style={{
        backgroundImage:
          "radial-gradient(circle at 20% 20%, color-mix(in srgb, var(--color-primary) 6%, transparent), transparent 50%), radial-gradient(circle at 80% 80%, color-mix(in srgb, var(--color-secondary) 5%, transparent), transparent 45%)",
      }}
    >
      {/* Left: brand hero */}
      <div
        className="relative hidden lg:flex flex-col justify-between p-14 border-r border-[rgba(128,128,128,0.2)] overflow-hidden"
      >
        <div
          className="absolute inset-0 opacity-30"
          style={{
            backgroundImage:
              "url(https://images.unsplash.com/photo-1599158150601-1417ebbaafdd?crop=entropy&cs=srgb&fm=jpg&q=85&w=1600)",
            backgroundSize: "cover",
            backgroundPosition: "center",
          }}
        />
        <div
          className="absolute inset-0"
          style={{
            background:
              "linear-gradient(180deg, color-mix(in srgb, var(--color-bg) 60%, transparent) 0%, color-mix(in srgb, var(--color-bg) 95%, transparent) 100%)",
          }}
        />
        <div className="relative z-10">
          <style>{`
            @keyframes gradientShift {
              0% { background-position: 0% 50%; }
              50% { background-position: 100% 50%; }
              100% { background-position: 0% 50%; }
            }
            .animate-playneml {
              background: linear-gradient(270deg, var(--color-primary), #ffffff, var(--color-secondary), var(--color-primary));
              background-size: 300% 300%;
              animation: gradientShift 4s ease infinite;
              -webkit-background-clip: text;
              -webkit-text-fill-color: transparent;
              filter: drop-shadow(0 0 12px rgba(var(--color-primary-rgb), 0.4));
            }
          `}</style>
          <div className="label-cap tracking-widest text-white/50">Season One</div>
          <div className="h-heading text-5xl font-bold mt-4 leading-[0.95] flex flex-col gap-1">
            <div className="text-white/90">Welcome to</div>
            <div className="animate-playneml text-7xl md:text-8xl py-1">
              PlayNeML.
            </div>
          </div>
          <p className="mt-6 max-w-md opacity-60 leading-relaxed text-lg">
            Real-time bidding. Dynamic matchmaking. A professional, multi-sport league management platform built for true competition.
          </p>
        </div>
        <div className="relative z-10 grid grid-cols-3 gap-6 pt-8 border-t border-[rgba(128,128,128,0.2)]">
          <div>
            <div className="label-cap">Teams</div>
            <div className="stat-num text-3xl mt-1">04</div>
          </div>
          <div>
            <div className="label-cap">Players</div>
            <div className="stat-num text-3xl mt-1">48</div>
          </div>
          <div>
            <div className="label-cap">Purse / Team</div>
            <div className="stat-num text-3xl mt-1 text-primary">₹100 Cr</div>
          </div>
        </div>
      </div>

      {/* Right: form */}
      <div className="flex items-center justify-center p-8 lg:p-14 relative z-10">
        <div className="w-full max-w-md">
          <div className="mb-8">
            <div className="label-cap">Sign in</div>
            <h1 className="h-heading text-4xl font-bold mt-2">Access Control</h1>
            <p className="opacity-50 mt-2 text-sm">
              Sign in as platform staff, or join a room for read-only viewer access.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="label-cap block mb-2">Email</label>
              <input
                data-testid="login-email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@neml.com"
                className="input"
                required
              />
            </div>
            <div>
              <label className="label-cap block mb-2">Password</label>
              <input
                data-testid="login-password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="input"
                required
              />
            </div>
            <button
              data-testid="login-submit"
              type="submit"
              disabled={loading}
              className="btn btn-primary w-full justify-center py-3 mt-2"
            >
              {loading ? "Signing in..." : "Enter Championship →"}
            </button>
          </form>

          <div className="mt-8 border-t border-[rgba(128,128,128,0.2)] pt-6">
            <div className="label-cap mb-3">Quick sign-in (demo)</div>
            <div className="grid grid-cols-3 gap-2">
              <button
                data-testid="quick-admin"
                type="button"
                onClick={() => quickFill("admin")}
                className="btn btn-ghost justify-center text-[10px]"
              >
                Admin
              </button>
              <button
                data-testid="quick-owner"
                type="button"
                onClick={() => quickFill("owner")}
                className="btn btn-ghost justify-center text-[10px]"
              >
                Team Owner
              </button>
              <button
                data-testid="quick-viewer"
                type="button"
                onClick={() => quickFill("viewer")}
                className="btn btn-ghost justify-center text-[10px]"
              >
                Viewer
              </button>
            </div>
            <p className="opacity-40 text-xs mt-3 leading-relaxed">
              Super Admin: admin@neml.com · Public viewer room: NEML-BAD1
            </p>
          </div>
        </div>
      </div>
    </div>
    </>
  );
}
