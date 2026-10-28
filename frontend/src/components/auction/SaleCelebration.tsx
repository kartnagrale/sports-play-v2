"use client";
import { useEffect, useState } from "react";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui";
import { PlayerAvatar } from "@/components/players/PlayerAvatar";
import { ConfirmedSale } from "@/lib/auction-live";
import { formatCr } from "@/lib/format";
export function SaleCelebration({ sale,onDismiss }: { sale: ConfirmedSale; onDismiss: () => void }) {
  const [leaving,setLeaving] = useState(false);
  useEffect(() => { const timer = window.setTimeout(() => setLeaving(true),6500); return () => window.clearTimeout(timer); },[]);
  useEffect(() => { if (!leaving) return; const timer = window.setTimeout(onDismiss,220); return () => window.clearTimeout(timer); },[leaving,onDismiss]);
  return <Dialog title={`SOLD · ${sale.player}`} description="Confirmed auction purchase" onClose={() => setLeaving(true)} className={`sold-dialog ${leaving ? "is-leaving" : ""}`}>
    <div className="sold-graphic" data-testid="sold-celebration"><div className="sale-particles" aria-hidden="true">{Array.from({length:16},(_,i) => <i key={i} style={{ "--x":`${Math.round(Math.cos(i*Math.PI/8)*(110+i%3*25))}px`, "--y":`${Math.round(Math.sin(i*Math.PI/8)*(110+i%3*25))}px`, "--turn":`${i*45}deg`, animationDelay:`${i%4*40}ms` } as React.CSSProperties} />)}</div><p className="sold-stamp">SOLD</p><div className="relative z-10 my-5 inline-flex"><PlayerAvatar name={sale.player} imageUrl={sale.photoUrl} size="xl" /></div><h3 className="sold-player">{sale.player}</h3><p className="mt-2 text-sm text-muted">A new chapter with</p><div className="sold-team"><PlayerAvatar name={sale.teamName} shape="square" size="sm" alt="" /><span>{sale.teamName}</span></div><p className="label-cap mt-6">Final selling price</p><p className="sold-price" data-testid="sold-price">{formatCr(Number(sale.amount))}</p><Button onClick={() => setLeaving(true)} className="mt-7 w-full">Continue to auction</Button><p className="mt-3 text-xs text-muted">Purchase confirmed · Squad and purse updated</p></div>
  </Dialog>;
}
