"use client";
import { Suspense, useEffect, useState, type FormEvent } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { BadgeCheck, ShieldX } from "lucide-react";
import { api } from "@/lib/api";
import { categoryLabel, type CertificateCategory } from "@/lib/certificates";
import { Button, Card, Field, Input, LoadingIndicator, StatusBadge } from "@/components/ui";

type Verification = { status: "VALID" | "REVOKED" | "INVALID"; reference?: string; recipient?: string; championship?: string; sport?: string; category?: CertificateCategory; issueDate?: string };
export default function VerifyPage() { return <Suspense fallback={<LoadingIndicator label="Opening certificate verification…" />}><VerificationForm /></Suspense>; }
function VerificationForm() {
  const params = useSearchParams(); const initial = params.get("reference") || "";
  const [reference, setReference] = useState(initial); const [lookup, setLookup] = useState(initial); const [retry, setRetry] = useState(0);
  const [result, setResult] = useState<Verification>(); const [busy, setBusy] = useState(false); const [error, setError] = useState("");
  useEffect(() => {
    if (!lookup) return; const controller = new AbortController(); const timer = setTimeout(() => {
      setBusy(true); setError(""); setResult(undefined);
      api.get<Verification>(`/certificate-verification/${encodeURIComponent(lookup.trim())}`, { signal: controller.signal }).then(r => { setResult(r.data); setBusy(false); }).catch(() => { if (!controller.signal.aborted) { setError("Verification is temporarily unavailable. Please try again."); setBusy(false); } });
    }, 0); return () => { clearTimeout(timer); controller.abort(); };
  }, [lookup, retry]);
  function submit(e: FormEvent) { e.preventDefault(); setLookup(reference.trim()); setRetry(n => n + 1); }
  return <main className="mx-auto flex min-h-screen max-w-2xl flex-col justify-center px-5 py-12"><Link href="/" className="mb-8 text-xl font-semibold text-primary">Play-Neml</Link><Card><p className="label-cap mb-3">Certificate authenticity</p><h1 className="page-title">Verify a certificate</h1><p className="my-4 text-sm text-muted">Enter the reference printed on the certificate or scan its QR code. This page shows only the details needed to confirm the award.</p><form onSubmit={submit} className="space-y-4"><Field label="Certificate reference">{props => <Input {...props} required maxLength={48} autoComplete="off" placeholder="PN-…" value={reference} onChange={e => setReference(e.target.value)} />}</Field><Button type="submit" loading={busy}>Verify certificate</Button></form>{error && <p className="mt-5 text-danger" role="alert">{error}</p>}{busy && <LoadingIndicator label="Checking certificate status…" />}{result && <section className="mt-6 rounded-xl border border-border p-5" aria-live="polite"><div className="mb-4 flex items-center gap-3">{result.status === "VALID" ? <BadgeCheck className="text-primary" /> : <ShieldX className="text-danger" />}<StatusBadge tone={result.status === "VALID" ? "success" : "danger"}>{result.status === "VALID" ? "Valid certificate" : result.status === "REVOKED" ? "Revoked certificate" : "Invalid / not found"}</StatusBadge></div>{result.status === "INVALID" ? <p className="text-sm text-muted">No publicly verifiable certificate matches this reference.</p> : <><h2 className="section-title break-words">{result.recipient}</h2><p className="mt-2">{result.championship}</p><p className="mt-1 text-sm text-muted">{result.category && categoryLabel[result.category]} · {result.sport} · Issued {result.issueDate}</p><p className="mt-4 break-all font-mono text-xs text-muted">{result.reference}</p>{result.status === "REVOKED" && <p className="mt-4 text-sm text-danger">This certificate was revoked and is no longer valid. Contact the championship administrator for a correction.</p>}</>}</section>}</Card></main>;
}
