"use client";

import { useEffect, useId, useRef, useState, type ReactNode } from "react";
import { Button } from ".";

export function Tooltip({ label, children }: { label: string; children: ReactNode }) {
  const id = useId();
  return <span className="tooltip-wrap" tabIndex={0} aria-describedby={id}>{children}<span id={id} role="tooltip" className="tooltip-content">{label}</span></span>;
}

/** Menu actions only. Arrow keys/Home/End move focus, Escape returns to trigger. */
export function DropdownMenu({ label, trigger, items }: { label: string; trigger: ReactNode; items: { label: string; onSelect: () => void; disabled?: boolean }[] }) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const button = useRef<HTMLButtonElement>(null);
  const id = useId();
  useEffect(() => {
    if (!open) return;
    ref.current?.querySelector<HTMLButtonElement>('[role="menuitem"]:not(:disabled)')?.focus();
    const close = (e: PointerEvent) => { if (!ref.current?.contains(e.target as Node)) setOpen(false); };
    const focus = (e: FocusEvent) => { if (!ref.current?.contains(e.target as Node)) setOpen(false); };
    document.addEventListener("pointerdown", close); document.addEventListener("focusin", focus);
    return () => { document.removeEventListener("pointerdown", close); document.removeEventListener("focusin", focus); };
  }, [open]);
  return <div ref={ref} className="relative" onKeyDown={event => {
    if (event.key === "Escape") { event.preventDefault(); setOpen(false); button.current?.focus(); }
    const options = [...(ref.current?.querySelectorAll<HTMLButtonElement>('[role="menuitem"]:not(:disabled)') || [])];
    if (["ArrowDown", "ArrowUp", "Home", "End"].includes(event.key)) {
      event.preventDefault(); if (!open) { setOpen(true); return; }
      const current = options.indexOf(document.activeElement as HTMLButtonElement);
      options[event.key === "Home" ? 0 : event.key === "End" ? options.length - 1 : (current + (event.key === "ArrowUp" ? -1 : 1) + options.length) % options.length]?.focus();
    }
  }}><button ref={button} type="button" className="account-trigger" aria-label={label} aria-haspopup="menu" aria-expanded={open} aria-controls={open ? id : undefined} onClick={() => setOpen(!open)}>{trigger}</button>
    {open && <div id={id} role="menu" aria-label={label} className="dropdown-panel">{items.map(item => <button key={item.label} type="button" role="menuitem" disabled={item.disabled} onClick={() => { setOpen(false); button.current?.focus(); item.onSelect(); }}>{item.label}</button>)}</div>}
  </div>;
}

export function Tabs({ label, tabs, value, onChange }: { label: string; tabs: { value: string; label: string; content: ReactNode; disabled?: boolean }[]; value: string; onChange: (value: string) => void }) {
  const id = useId();
  return <div><div role="tablist" aria-label={label} className="ui-tabs" onKeyDown={event => {
    if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
    event.preventDefault(); const enabled = tabs.filter(tab => !tab.disabled); const current = enabled.findIndex(tab => tab.value === value);
    const next = enabled[event.key === "Home" ? 0 : event.key === "End" ? enabled.length - 1 : (current + (event.key === "ArrowLeft" ? -1 : 1) + enabled.length) % enabled.length];
    if (next) { onChange(next.value); document.getElementById(`${id}-${next.value}`)?.focus(); }
  }}>{tabs.map(tab => <Button key={tab.value} variant="ghost" role="tab" id={`${id}-${tab.value}`} aria-controls={`${id}-${tab.value}-panel`} aria-selected={value === tab.value} tabIndex={value === tab.value ? 0 : -1} disabled={tab.disabled} onClick={() => onChange(tab.value)}>{tab.label}</Button>)}</div>{tabs.map(tab => <div key={tab.value} role="tabpanel" id={`${id}-${tab.value}-panel`} aria-labelledby={`${id}-${tab.value}`} hidden={value !== tab.value} tabIndex={0} className="pt-4">{tab.content}</div>)}</div>;
}
