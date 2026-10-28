"use client";

import { useId, type ReactNode, type ComponentProps } from "react";
import { clsx } from "clsx";
import { AlertCircle, ChevronLeft, ChevronRight, Inbox, Loader2, Search } from "lucide-react";

type ButtonProps = ComponentProps<"button"> & { variant?: "primary" | "ghost" | "danger" | "warn" | "cyan"; loading?: boolean };
const buttonClasses = { primary: "btn-primary", ghost: "btn-ghost", danger: "btn-danger", warn: "btn-warn", cyan: "btn-cyan" };
export function Button({ variant = "primary", loading, disabled, className, children, type = "button", ...props }: ButtonProps) {
  return <button {...props} type={type} disabled={disabled || loading} aria-busy={loading || undefined} className={clsx("btn", buttonClasses[variant], className)}>{loading && <Loader2 size={16} className="animate-spin" aria-hidden="true" />}{children}</button>;
}
export function IconButton({ label, children, className, ...props }: ButtonProps & { label: string }) {
  return <Button {...props} variant={props.variant || "ghost"} aria-label={label} className={clsx("icon-button", className)}>{children}</Button>;
}
export function Card({ className, ...props }: ComponentProps<"section">) {
  return <section {...props} className={clsx("ui-card", className)} />;
}
export function SectionHeader({ title, eyebrow, description, actions, as: Tag = "h2" }: { title: string; eyebrow?: string; description?: string; actions?: ReactNode; as?: "h1" | "h2" | "h3" }) {
  return <div className="section-header"><div className="min-w-0">{eyebrow && <p className="label-cap mb-2">{eyebrow}</p>}<Tag className={Tag === "h1" ? "page-title" : "section-title"}>{title}</Tag>{description && <p className="mt-2 text-sm text-muted">{description}</p>}</div>{actions && <div className="flex min-w-0 max-w-full flex-wrap gap-2">{actions}</div>}</div>;
}
export function PageHeader(props: Omit<ComponentProps<typeof SectionHeader>, "as">) { return <SectionHeader {...props} as="h1" />; }
export function MetricCard({ label, value, detail, icon }: { label: string; value: ReactNode; detail?: string; icon?: ReactNode }) {
  return <Card><div className="flex items-center justify-between gap-3"><h3 className="label-cap">{label}</h3><span className="text-muted" aria-hidden="true">{icon}</span></div><p className="stat-num mt-3 text-3xl">{value}</p>{detail && <p className="mt-2 text-sm text-muted">{detail}</p>}</Card>;
}
export type StatusTone = "neutral" | "success" | "warning" | "danger" | "info" | "live";
const statusClasses: Record<StatusTone, string> = { neutral: "status-neutral", success: "status-success", warning: "status-warning", danger: "status-danger", info: "status-info", live: "status-live" };
export function StatusBadge({ tone = "neutral", children, live = false }: { tone?: StatusTone; children: ReactNode; live?: boolean }) {
  return <span className={clsx("status-badge", statusClasses[tone])}>{live && <span className="status-dot" aria-hidden="true" />}{children}</span>;
}
export const Input = ({ className, ...props }: ComponentProps<"input">) => <input {...props} className={clsx("input", className)} />;
export const Select = ({ className, ...props }: ComponentProps<"select">) => <select {...props} className={clsx("input", className)} />;
export const Textarea = ({ className, ...props }: ComponentProps<"textarea">) => <textarea {...props} className={clsx("input min-h-28 resize-y", className)} />;
export function Checkbox({ label, className, ...props }: Omit<ComponentProps<"input">, "type"> & { label: ReactNode }) {
  return <label className={clsx("flex items-center gap-3 text-sm", className)}><input {...props} type="checkbox" className="ui-checkbox" /><span>{label}</span></label>;
}
/** Render prop guarantees label, hint and error are associated with the control. */
export function Field({ label, hint, error, children }: { label: string; hint?: string; error?: string; children: (props: { id: string; "aria-describedby"?: string; "aria-invalid"?: boolean }) => ReactNode }) {
  const id = useId();
  return <div><label htmlFor={id} className="field-label">{label}</label>{children({ id, "aria-describedby": hint || error ? `${id}-help` : undefined, "aria-invalid": !!error || undefined })}{(hint || error) && <p id={`${id}-help`} className={clsx("mt-2 text-xs", error ? "text-danger" : "text-muted")}>{error || hint}</p>}</div>;
}
export function SearchInput({ label = "Search", className, ...props }: ComponentProps<"input"> & { label?: string }) {
  return <div className={clsx("relative", className)}><Search size={17} className="pointer-events-none absolute left-3 top-3.5 text-muted" aria-hidden="true" /><Input {...props} type="search" aria-label={label} className="pl-10" /></div>;
}
export function Skeleton({ className, ...props }: ComponentProps<"div">) { return <div {...props} aria-hidden="true" className={clsx("skeleton rounded-lg", className)} />; }
export function LoadingIndicator({ label = "Loading…" }: { label?: string }) { return <div role="status" className="flex items-center justify-center gap-3 p-6 text-sm text-muted"><Loader2 aria-hidden="true" size={20} className="animate-spin text-primary" />{label}</div>; }
export function EmptyState({ title, description, action, icon = <Inbox size={26} /> }: { title: string; description?: string; action?: ReactNode; icon?: ReactNode }) {
  return <div className="empty-state"><span className="empty-state-icon" aria-hidden="true">{icon}</span><h2 className="section-title mt-4">{title}</h2>{description && <p className="mx-auto mt-2 max-w-md text-sm text-muted">{description}</p>}{action && <div className="mt-5">{action}</div>}</div>;
}
export function ErrorState({ title = "Something went wrong", description, onRetry }: { title?: string; description?: string; onRetry?: () => void }) {
  return <div role="alert"><EmptyState title={title} description={description} icon={<AlertCircle className="text-danger" size={26} />} action={onRetry && <Button variant="ghost" onClick={onRetry}>Try again</Button>} /></div>;
}
export function DataTable({ caption, children, className }: { caption: string; children: ReactNode; className?: string }) {
  return <div className="table-scroll" role="region" aria-label={caption} tabIndex={0}><table className={clsx("data-table", className)}><caption className="sr-only">{caption}</caption>{children}</table></div>;
}
export function Pagination({ page, pageCount, onPageChange }: { page: number; pageCount: number; onPageChange: (page: number) => void }) {
  return <nav aria-label="Pagination" className="flex items-center justify-end gap-3 p-4 text-sm"><IconButton label="Previous page" disabled={page <= 1} onClick={() => onPageChange(page - 1)}><ChevronLeft size={16} /></IconButton><span aria-live="polite">Page {page} of {Math.max(1, pageCount)}</span><IconButton label="Next page" disabled={page >= pageCount} onClick={() => onPageChange(page + 1)}><ChevronRight size={16} /></IconButton></nav>;
}
