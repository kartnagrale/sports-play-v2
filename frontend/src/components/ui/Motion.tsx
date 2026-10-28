"use client";
import { useEffect, useLayoutEffect, useRef, type ReactNode } from "react";
export function MotionReveal({ children, className = "" }: { children: ReactNode; className?: string }) { return <div className={`motion-enter ${className}`}>{children}</div>; }
export function UpdatedValue({ children, revision, announce = false }: { children: ReactNode; revision: string | number; announce?: boolean }) { return <span key={revision} className="motion-highlight rounded px-1 tabular-nums" aria-live={announce ? "polite" : undefined} aria-atomic={announce || undefined}>{children}</span>; }
/** Interruptible FLIP: begin a new move at the current visual position during bursts. */
export function ReorderGroup({ children, revision, className = "" }: { children: ReactNode; revision: string | number; className?: string }) {
  const root = useRef<HTMLDivElement>(null);
  const positions = useRef(new Map<string,{x:number;y:number}>());
  const animations = useRef<Animation[]>([]);
  useLayoutEffect(() => {
    if (!root.current) return;
    const origin = root.current.getBoundingClientRect();
    const measurements = [...root.current.querySelectorAll<HTMLElement>("[data-motion-id]")].map(node => {
      const rect = node.getBoundingClientRect();
      const raw = getComputedStyle(node).transform;
      const transform = raw === "none" ? new DOMMatrixReadOnly() : new DOMMatrixReadOnly(raw);
      return { node, id: node.dataset.motionId!, x: rect.x-origin.x-transform.m41, y: rect.y-origin.y-transform.m42, dx: transform.m41, dy: transform.m42 };
    });
    animations.current.forEach(animation => animation.cancel()); animations.current = [];
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    measurements.forEach(({ node,id,x,y,dx,dy }) => {
      const before = positions.current.get(id);
      if (!reduced && before && (before.x+dx !== x || before.y+dy !== y)) animations.current.push(node.animate([{ transform: `translate(${before.x+dx-x}px,${before.y+dy-y}px)` }, { transform: "translate(0,0)" }],{ duration:220,easing:"cubic-bezier(.2,.8,.2,1)" }));
    });
    positions.current = new Map(measurements.map(item => [item.id,{x:item.x,y:item.y}]));
  },[revision]);
  useEffect(() => {
    const media = window.matchMedia("(prefers-reduced-motion: reduce)"); const stop = () => { if (media.matches) animations.current.forEach(animation => animation.cancel()); };
    media.addEventListener("change",stop);
    return () => { media.removeEventListener("change",stop); animations.current.forEach(animation => animation.cancel()); };
  },[]);
  return <div ref={root} className={className}>{children}</div>;
}
