"use client";

import { useEffect, useState } from "react";
import { Dribbble, Feather, Circle, Trophy } from "lucide-react";

export default function SplashScreen({ onComplete }: { onComplete: () => void }) {
  const [progress, setProgress] = useState(0);
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    // Animate progress to 100 over 2 seconds
    const duration = 2000;
    const interval = 20;
    const steps = duration / interval;
    let currentStep = 0;

    const timer = setInterval(() => {
      currentStep++;
      const newProgress = Math.min((currentStep / steps) * 100, 100);
      setProgress(newProgress);

      if (currentStep >= steps) {
        clearInterval(timer);
        // Wait a moment at 100% then fade out
        setTimeout(() => {
          setIsVisible(false);
          // Wait for fade out animation to finish before removing from DOM
          setTimeout(onComplete, 500);
        }, 300);
      }
    }, interval);

    return () => clearInterval(timer);
  }, [onComplete]);

  return (
    <div
      className={`fixed inset-0 z-50 flex items-center justify-center bg-bg transition-opacity duration-500 overflow-hidden ${
        isVisible ? "opacity-100" : "opacity-0 pointer-events-none"
      }`}
    >
      {/* Background Image & Overlay */}
      <div 
        className="absolute inset-0 z-0 opacity-40 mix-blend-luminosity"
        style={{
          backgroundImage: "url(https://images.unsplash.com/photo-1518605368461-1e1e38ce8f41?auto=format&fit=crop&q=80&w=1920)",
          backgroundSize: "cover",
          backgroundPosition: "center",
        }}
      />
      <div 
        className="absolute inset-0 z-0"
        style={{
          background: "radial-gradient(circle at 50% 50%, color-mix(in srgb, var(--color-primary) 20%, transparent) 0%, var(--color-bg) 80%)"
        }}
      />

      <div className="relative z-10 flex flex-col items-center max-w-xl w-full px-6">
        
        {/* Main Logo Text */}
        <div className="h-heading font-bold flex flex-col items-center text-center">
          <style>{`
            @keyframes gradientShiftSplash {
              0% { background-position: 0% 50%; }
              50% { background-position: 100% 50%; }
              100% { background-position: 0% 50%; }
            }
            @keyframes slideInLeft {
              0% { transform: translateX(-150px); opacity: 0; }
              100% { transform: translateX(0); opacity: 1; }
            }
            @keyframes slideInRight {
              0% { transform: translateX(150px); opacity: 0; }
              100% { transform: translateX(0); opacity: 1; }
            }
            .animate-slide-left {
              animation: slideInLeft 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
            }
            .animate-slide-right {
              animation: slideInRight 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
            }
            .animate-playneml-splash {
              background: linear-gradient(270deg, var(--color-primary), #ffffff, var(--color-secondary), var(--color-primary));
              background-size: 300% 300%;
              animation: gradientShiftSplash 3s ease infinite;
              -webkit-background-clip: text;
              -webkit-text-fill-color: transparent;
              filter: drop-shadow(0 0 20px rgba(var(--color-primary-rgb), 0.6));
            }
          `}</style>
          <div className="text-7xl md:text-8xl py-2 tracking-tighter flex overflow-hidden p-4 -m-4">
            <span className="animate-slide-left animate-playneml-splash inline-block">Play</span>
            <span className="animate-slide-right animate-playneml-splash inline-block">NeML</span>
          </div>
          
          <div className="text-primary font-bold tracking-[0.3em] text-sm md:text-base mt-2 drop-shadow-glow">
            PLAY • COMPETE • ENJOY
          </div>
          <div className="flex items-center gap-4 mt-6 opacity-60 w-full justify-center">
            <div className="h-[1px] bg-white/20 flex-1 max-w-[50px]"></div>
            <div className="tracking-[0.2em] text-xs font-semibold uppercase">Multi Game Outdoor</div>
            <div className="h-[1px] bg-white/20 flex-1 max-w-[50px]"></div>
          </div>
        </div>

        {/* Glowing Icons */}
        <div className="flex items-center gap-8 mt-10">
          <div className="p-3 rounded-full bg-white/5 border border-white/10 shadow-[0_0_15px_rgba(0,255,100,0.2)] text-[#00ff66]">
            <Trophy size={28} />
          </div>
          <div className="p-3 rounded-full bg-white/5 border border-white/10 shadow-[0_0_15px_rgba(0,150,255,0.2)] text-[#0096ff]">
            <Circle size={28} />
          </div>
          <div className="p-3 rounded-full bg-white/5 border border-white/10 shadow-[0_0_15px_rgba(255,100,0,0.2)] text-[#ff6400]">
            <Dribbble size={28} />
          </div>
          <div className="p-3 rounded-full bg-white/5 border border-white/10 shadow-[0_0_15px_rgba(255,255,255,0.2)] text-white">
            <Feather size={28} />
          </div>
        </div>

        {/* Loading Bar */}
        <div className="w-full mt-14 flex flex-col items-center">
          <div className="tracking-[0.3em] text-xs font-bold uppercase mb-4 opacity-80">
            Loading...
          </div>
          <div className="w-full max-w-sm h-3 rounded-full bg-white/10 overflow-hidden relative border border-white/5">
            <div 
              className="absolute top-0 left-0 h-full bg-gradient-to-r from-primary to-secondary transition-all duration-75 ease-linear shadow-[0_0_10px_rgba(var(--color-primary-rgb),0.8)]"
              style={{ width: `${progress}%` }}
            />
          </div>
          <div className="mt-4 font-mono text-primary drop-shadow-glow font-bold">
            {Math.round(progress)}%
          </div>
        </div>

        {/* Bottom Tagline */}
        <div className="mt-16 text-center opacity-40">
          <div className="mb-2 text-xl opacity-60">🏔️</div>
          <div className="tracking-[0.15em] text-[10px] font-bold uppercase">
            One Place. Many Games. Endless Fun.
          </div>
        </div>
      </div>
    </div>
  );
}
