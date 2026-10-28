"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { UserRound } from "lucide-react";
import { clsx } from "clsx";
import Image from "next/image";

export type AvatarSize = "sm" | "md" | "lg" | "xl";
export interface PlayerAvatarProps { name?: string; imageUrl?: string | null; size?: AvatarSize; shape?: "circle" | "square"; alt?: string; loading?: boolean }
const sizes = { sm: "h-8 w-8 text-xs", md: "h-11 w-11 text-sm", lg: "h-20 w-20 text-2xl", xl: "h-32 w-32 text-4xl" };

export function PlayerAvatar({ name, imageUrl, size = "md", shape = "circle", alt, loading = false }: PlayerAvatarProps) {
  // Keyed image state resets when a future Phase 4 URL changes, including A → B → A.
  return <AvatarImage key={imageUrl || "placeholder"} {...{ name, imageUrl, size, shape, alt, loading }} />;
}
function AvatarImage({ name, imageUrl, size, shape, alt, loading }: PlayerAvatarProps) {
  const [failed, setFailed] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [privateUrl,setPrivateUrl]=useState<string|null>(null);
  const isPrivate=!!imageUrl?.startsWith("/championships/");
  useEffect(()=>{
    if(!isPrivate||!imageUrl)return;
    const controller=new AbortController();let objectUrl:string|null=null;
    api.get<Blob>(imageUrl,{responseType:"blob",signal:controller.signal}).then(({data})=>{if(!controller.signal.aborted){objectUrl=URL.createObjectURL(data);setPrivateUrl(objectUrl);}}).catch(()=>{if(!controller.signal.aborted)setFailed(true);});
    return()=>{controller.abort();if(objectUrl)URL.revokeObjectURL(objectUrl);};
  },[imageUrl,isPrivate]);
  const safeUrl = isPrivate?privateUrl:imageUrl && (/^https?:\/\//i.test(imageUrl) || /^\/(?!\/)/.test(imageUrl)) ? imageUrl : null;
  const initials = name?.trim().split(/\s+/).filter(Boolean).filter((_, i, list) => i === 0 || i === list.length - 1).map(part => Array.from(part)[0]).join("").toLocaleUpperCase();
  const label = alt ?? (name ? `${name} avatar` : "Player avatar");
  return <span role="img" aria-label={label || undefined} aria-hidden={label === "" || undefined} aria-busy={loading || (safeUrl && !loaded && !failed) || undefined} className={clsx("relative inline-grid shrink-0 place-items-center overflow-hidden border border-primary/20 bg-primary/10 font-semibold text-primary", sizes[size], shape === "square" ? "rounded-2xl" : "rounded-full", loading && "skeleton")}>
    <span aria-hidden="true">{initials || <UserRound size={size === "xl" ? 52 : size === "lg" ? 34 : 18} strokeWidth={1.5} />}</span>
    {safeUrl && !failed && !loading && <Image unoptimized width={128} height={128} src={safeUrl} alt="" referrerPolicy="no-referrer" loading="lazy" onLoad={() => setLoaded(true)} onError={() => setFailed(true)} className={clsx("absolute inset-0 h-full w-full object-cover transition-opacity", loaded ? "opacity-100" : "opacity-0")} />}
  </span>;
}

export function PlayerIdentity({ name, metadata, imageUrl, size = "sm", shape }: { name: string; metadata?: React.ReactNode; imageUrl?: string | null; size?: AvatarSize; shape?: "circle" | "square" }) {
  return <span className="inline-flex min-w-0 items-center gap-3"><PlayerAvatar name={name} imageUrl={imageUrl} size={size} shape={shape} alt="" /><span className="min-w-0"><span className="block font-medium">{name}</span>{metadata && <span className="mt-0.5 block text-xs text-muted">{metadata}</span>}</span></span>;
}
