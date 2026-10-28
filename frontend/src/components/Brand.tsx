import { clsx } from "clsx";

/** Original track / play monogram. Product branding, not an official NEML logo. */
export function BrandMark({ className }: { className?: string }) {
  return <svg viewBox="0 0 40 40" fill="none" aria-hidden="true" className={clsx("brand-mark", className)}>
    <rect width="40" height="40" rx="12" fill="currentColor" />
    <path d="M11 28V12h9a8 8 0 0 1 0 16h-3v-5h3a3 3 0 1 0 0-6h-4v11z" fill="var(--color-on-primary)" />
    <path d="m24 17 7 3-7 3z" fill="var(--color-on-primary)" />
  </svg>;
}

export function Brand({ compact = false }: { compact?: boolean }) {
  return <span className="inline-flex items-center gap-3" data-testid="brand-name">
    <BrandMark />{!compact && <span className="brand-wordmark">Play<span className="text-primary">-Neml</span></span>}
    {compact && <span className="sr-only">Play-Neml</span>}
  </span>;
}
