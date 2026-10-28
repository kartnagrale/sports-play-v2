"use client";

import { CertificateBulkDialog } from "@/components/certificates/CertificateBulkDialog";
import Image from "next/image";
import { useCallback, useEffect, useState } from "react";
import { Award, CheckCircle2, Download, Eye, Medal, Plus, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { api } from "@/lib/api";
import { useChampionship } from "@/lib/championship";
import { useNavigation } from "@/lib/navigation";
import { certificateBase, certificateError, categoryLabel, saveCertificate, type CertificateCategory, type CertificateEligibility, type CertificateHistory, type CertificateIssueRequest, type IssuedCertificate, type RecipientType } from "@/lib/certificates";
import { Button, Card, Checkbox, DataTable, EmptyState, ErrorState, Field, LoadingIndicator, MetricCard, PageHeader, Pagination, SearchInput, Select, StatusBadge, Textarea } from "@/components/ui";
import { Dialog } from "@/components/ui/Dialog";
import { PlayerIdentity } from "@/components/players/PlayerAvatar";

const steps = ["Award", "Event", "Recipient", "Preview", "Generate"];
export default function CertificatesPage() {
  const active = useChampionship(s => s.active);
  const authorized = useNavigation(s => s.role) === "CHAMPIONSHIP_ADMIN";
  if (!authorized) return <EmptyState title="Administrator access required" description="Certificate management is available to the assigned championship administrator." />;
  if (!active) return <EmptyState title="Select a championship" />;
  return <CertificateWorkspace key={active.id} cid={active.id} name={active.name} />;
}
function CertificateWorkspace({ cid, name }: { cid: string; name: string }) {
  const [history, setHistory] = useState<CertificateHistory>();
  const [error, setError] = useState(""); const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState(""); const [category, setCategory] = useState(""); const [status, setStatus] = useState(""); const [page, setPage] = useState(0);
  const [bulk, setBulk] = useState(false); const [refresh, setRefresh] = useState(0); const [wizard, setWizard] = useState(false); const [reissue, setReissue] = useState<IssuedCertificate>();
  const [revoke, setRevoke] = useState<IssuedCertificate>(); const [reason, setReason] = useState(""); const [busy, setBusy] = useState(false);
  const [approve, setApprove] = useState(false); const [justification, setJustification] = useState("");
  const [view, setView] = useState<{ url: string; imageUrl: string; title: string; revoked: boolean }>();
  useEffect(() => { return () => { if (view) { URL.revokeObjectURL(view.url); URL.revokeObjectURL(view.imageUrl); } }; }, [view]);
  useEffect(() => {
    const controller = new AbortController();
    const timer = setTimeout(() => { setLoading(true); setError(""); api.get<CertificateHistory>(certificateBase(cid), { params: { search, category, status, page }, signal: controller.signal })
      .then(r => { setHistory(r.data); setLoading(false); }).catch(e => { if (!controller.signal.aborted) { setError(certificateError(e)); setLoading(false); } }); }, 200);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [cid, search, category, status, page, refresh]);
  const updated = () => setRefresh(x => x + 1);
  const openWizard = (old?: IssuedCertificate) => { setReissue(old); setWizard(true); };
  async function approveResults() {
    setBusy(true); try { await api.post(`${certificateBase(cid)}/approvals`, { justification }); toast.success("Competition results approved for certificates"); setApprove(false); setJustification(""); updated(); }
    catch (e) { toast.error(certificateError(e)); } finally { setBusy(false); }
  }
  async function revokeCertificate() {
    if (!revoke) return; setBusy(true);
    try { await api.post(`${certificateBase(cid)}/${revoke.certificate.id}/revoke`, { reason }); toast.success("Certificate revoked"); setRevoke(undefined); setReason(""); updated(); }
    catch (e) { toast.error(certificateError(e)); } finally { setBusy(false); }
  }
  async function show(issued: IssuedCertificate) {
    try { const [pdf, png] = await Promise.all([api.get<Blob>(`${certificateBase(cid)}/${issued.certificate.id}/pdf?inline=true`, { responseType: "blob" }), api.get<Blob>(`${certificateBase(cid)}/${issued.certificate.id}/image`, { responseType: "blob" })]); setView({ url: URL.createObjectURL(pdf.data), imageUrl: URL.createObjectURL(png.data), title: issued.certificate.reference, revoked: issued.status === "REVOKED" }); }
    catch (e) { toast.error(certificateError(e)); }
  }
  return <div className="portal-page space-y-6">
    <PageHeader eyebrow="Recognition & awards" title="Certificates" description={`Recognize achievement at ${name}. Every award is backed by approved competition results.`} actions={<><Button variant="ghost" onClick={() => setBulk(true)}>Bulk generation</Button><Button variant="ghost" onClick={() => setApprove(true)}><ShieldCheck size={16} />Approve results</Button><Button onClick={() => openWizard()}><Plus size={16} />Generate certificate</Button></>} />
    <div className="grid grid-cols-2 gap-3 xl:grid-cols-5">{[["Total issued", "TOTAL"], ["Winners", "WINNER"], ["Runners-up", "RUNNER_UP"], ["Participation", "PARTICIPATION"], ["Revoked", "REVOKED"]].map(([label, key]) => <MetricCard key={key} label={label} value={history ? history.counts[key] || 0 : "—"} icon={<Medal size={18} />} />)}</div>
    <Card className="flex flex-wrap items-center justify-between gap-4 border-primary/20 bg-primary/5"><div className="flex items-center gap-4"><Award className="shrink-0 text-primary" size={30} /><div><h2 className="section-title">Recognition with a record</h2><p className="mt-1 text-sm text-muted">Gold for champions. Silver for runners-up. Recognition for verified participation.</p></div></div><StatusBadge tone="info">Approved results · Immutable history</StatusBadge></Card>
    <Card><div className="mb-5 flex flex-wrap items-end justify-between gap-3"><div><p className="label-cap mb-2">Issuance activity</p><h2 className="section-title">Certificate history</h2></div><p className="text-sm text-muted">{history?.total || 0} matching records</p></div>
      <div className="mb-5 grid gap-3 md:grid-cols-3"><SearchInput label="Search certificate history" placeholder="Recipient name or certificate reference" value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} /><Select aria-label="Filter category" value={category} onChange={e => { setCategory(e.target.value); setPage(0); }}><option value="">All categories</option>{Object.entries(categoryLabel).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</Select><Select aria-label="Filter status" value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}><option value="">All statuses</option><option value="ISSUED">Issued</option><option value="REVOKED">Revoked</option></Select></div>
      {loading ? <LoadingIndicator label="Loading certificate history…" /> : error ? <ErrorState description={error} onRetry={updated} /> : !history?.items.length ? <EmptyState title="No certificates found" description="Approve completed results, then generate the first award for an eligible recipient." action={<Button onClick={() => openWizard()}>Generate certificate</Button>} /> : <><DataTable caption="Issued certificates"><thead><tr><th>Certificate / recipient</th><th>Event / award</th><th>Issued</th><th>Status</th><th>Actions</th></tr></thead><tbody>{history.items.map(item => <tr key={item.certificate.id}><td><p className="font-medium">{item.certificate.recipient.name}</p><p className="mt-1 break-all font-mono text-xs text-muted">{item.certificate.reference}</p><p className="mt-1 text-xs text-muted">{item.certificate.recipient.type}</p></td><td><StatusBadge>{categoryLabel[item.certificate.category]}</StatusBadge><p className="mt-2 text-xs text-muted">{item.certificate.championship} · {item.certificate.sport}</p></td><td><p>{new Date(item.certificate.issuedAt).toLocaleDateString()}</p><p className="mt-1 text-xs text-muted">{item.certificate.issuer}</p></td><td><StatusBadge tone={item.status === "REVOKED" ? "danger" : "success"}>{item.status}</StatusBadge>{item.revocationReason && <p className="mt-2 max-w-xs text-xs text-muted">{item.revocationReason}</p>}</td><td><div className="flex flex-wrap gap-2"><Button variant="ghost" aria-label={`View ${item.certificate.reference}`} onClick={() => void show(item)}><Eye size={15} /></Button><Button variant="ghost" aria-label={`Download ${item.certificate.reference}`} onClick={() => void saveCertificate(cid, item).catch(e => toast.error(certificateError(e)))}><Download size={15} /></Button>{item.status === "ISSUED" ? <Button variant="danger" onClick={() => { setRevoke(item); setReason(""); }}>Revoke</Button> : <Button variant="ghost" onClick={() => openWizard(item)}>Reissue</Button>}</div></td></tr>)}</tbody></DataTable><Pagination page={page + 1} pageCount={Math.ceil(history.total / 25)} onPageChange={n => setPage(n - 1)} /></>}
    </Card>
    {approve && <Dialog title="Approve competition results" onClose={() => !busy && setApprove(false)} description="All fixtures and the decisive final must be completed. The server checks the sport results and participation records before creating an immutable approval."><Field label="Approval justification" hint="At least 10 characters. This is recorded in the audit history.">{props => <Textarea {...props} maxLength={1000} value={justification} onChange={e => setJustification(e.target.value)} />}</Field><div className="mt-5 flex justify-end gap-2"><Button variant="ghost" disabled={busy} onClick={() => setApprove(false)}>Cancel</Button><Button loading={busy} disabled={justification.trim().length < 10} onClick={() => void approveResults()}>Approve finalized results</Button></div></Dialog>}
    {revoke && <Dialog title="Revoke certificate" onClose={() => !busy && setRevoke(undefined)} description={`Revoke ${revoke.certificate.reference} for ${revoke.certificate.recipient.name}. The original certificate and audit history are retained.`}><Field label="Revocation reason" hint="At least 10 characters. Revoked certificates cannot verify as valid.">{props => <Textarea {...props} maxLength={1000} value={reason} onChange={e => setReason(e.target.value)} />}</Field><div className="mt-5 flex justify-end gap-2"><Button variant="ghost" disabled={busy} onClick={() => setRevoke(undefined)}>Cancel</Button><Button variant="danger" loading={busy} disabled={reason.trim().length < 10} onClick={() => void revokeCertificate()}>Confirm revocation</Button></div></Dialog>}
    {view && <Dialog title={view.title} onClose={() => setView(undefined)} className="!w-[min(1000px,calc(100vw-2rem))] !max-w-5xl">{view.revoked && <p role="status" className="mb-3 text-danger">Revoked certificate. This is the original historical PDF.</p>}<PdfPreview url={view.url} imageUrl={view.imageUrl} /></Dialog>}
    {bulk && <CertificateBulkDialog cid={cid} onClose={() => setBulk(false)} onIssued={updated} />}
    {wizard && <CertificateWizard cid={cid} reissue={reissue} onClose={() => setWizard(false)} onIssued={updated} />}
  </div>;
}
function PdfPreview({ url, imageUrl }: { url: string; imageUrl: string }) {
  return <div className="space-y-3"><Image unoptimized width={1286} height={910} alt="Certificate preview rendered from the actual PDF" src={imageUrl} className="h-auto w-full rounded-xl border border-border bg-white" /><a className="text-sm text-primary underline" href={url} target="_blank" rel="noreferrer">Open PDF in a new tab</a></div>;
}
function CertificateWizard({ cid, reissue, onClose, onIssued }: { cid: string; reissue?: IssuedCertificate; onClose: () => void; onIssued: () => void }) {
  const [step, setStep] = useState(0); const [category, setCategory] = useState<CertificateCategory>(reissue?.certificate.category || "WINNER");
  const [type, setType] = useState<RecipientType>(reissue?.certificate.recipient.type || "TEAM"); const [data, setData] = useState<CertificateEligibility>();
  const [recipient, setRecipient] = useState(""); const [requestId, setRequestId] = useState(""); const [reason, setReason] = useState("");
  const [preview, setPreview] = useState(""); const [previewImage, setPreviewImage] = useState(""); const [error, setError] = useState(""); const [busy, setBusy] = useState(false); const [loading, setLoading] = useState(true);
  const [issued, setIssued] = useState<IssuedCertificate>(); const [confirmed, setConfirmed] = useState(false);
  useEffect(() => { return () => { if (preview) URL.revokeObjectURL(preview); if (previewImage) URL.revokeObjectURL(previewImage); }; }, [preview, previewImage]);
  const load = useCallback(() => {
    const controller = new AbortController(); setLoading(true); setError("");
    api.get<CertificateEligibility>(`${certificateBase(cid)}/eligibility`, { params: { category, recipientType: type }, signal: controller.signal }).then(r => { setData(r.data); setLoading(false); }).catch(e => { if (!controller.signal.aborted) { setError(certificateError(e)); setLoading(false); } });
    return () => controller.abort();
  }, [cid, category, type]);
  useEffect(() => { let cleanup: (() => void) | undefined; const timer = setTimeout(() => { cleanup = load(); }, 0); return () => { clearTimeout(timer); cleanup?.(); }; }, [load]);
  const payload = (): CertificateIssueRequest => ({ requestId, approvalId: data!.approval!.id, category, recipientType: type, recipientId: recipient, ...(reissue ? { reissuedFrom: reissue.certificate.id, reason } : {}) });
  async function renderPreview() {
    setBusy(true); setError("");
    try { const [pdf, png] = await Promise.all([api.post<Blob>(`${certificateBase(cid)}/preview`, payload(), { responseType: "blob" }), api.post<Blob>(`${certificateBase(cid)}/preview?format=png`, payload(), { responseType: "blob" })]); setPreview(URL.createObjectURL(pdf.data)); setPreviewImage(URL.createObjectURL(png.data)); setStep(3); }
    catch (e) { setError(certificateError(e)); } finally { setBusy(false); }
  }
  async function generate() {
    setBusy(true); setError("");
    try { const r = await api.post<IssuedCertificate>(certificateBase(cid), payload()); setIssued(r.data); onIssued(); toast.success("Certificate issued"); }
    catch (e) { setError(certificateError(e)); } finally { setBusy(false); }
  }
  const chosen = data?.recipients.find(r => r.id === recipient);
  return <Dialog title={reissue ? "Reissue certificate" : "Generate certificate"} className="!w-[min(900px,calc(100vw-2rem))] !max-w-4xl" onClose={() => !busy && onClose()} description="Create a print-ready award from approved results.">
    <ol className="mb-6 flex gap-1 overflow-x-auto pb-2" aria-label="Generation steps">{steps.map((label, index) => <li key={label} aria-current={index === step ? "step" : undefined} className={`flex min-w-20 flex-1 flex-col gap-2 border-t-2 pt-3 text-xs ${index === step ? "border-primary font-semibold text-primary" : "border-border text-muted"}`}><span>{String(index + 1).padStart(2, "0")}</span>{label}</li>)}</ol>
    {error && <p role="alert" className="mb-4 rounded-lg border border-danger/30 p-3 text-sm text-danger">{error}</p>}
    {step === 0 && <div className="space-y-5"><div className="grid gap-3 sm:grid-cols-3">{(Object.keys(categoryLabel) as CertificateCategory[]).map(c => <button key={c} type="button" disabled={!!reissue} aria-pressed={category === c} onClick={() => { setCategory(c); setType("TEAM"); setRecipient(""); setPreview(""); }} className={`rounded-xl border p-5 text-left focus-visible:outline focus-visible:outline-primary ${category === c ? "border-primary bg-primary/10" : "border-border bg-surface"}`}><Medal className={c === "WINNER" ? "text-warning" : c === "RUNNER_UP" ? "text-muted" : "text-primary"} /><span className="mt-4 block font-semibold">{categoryLabel[c]}</span><span className="mt-2 block text-xs text-muted">{c === "PARTICIPATION" ? "Verified participation in completed matches" : "Team position from the approved championship final"}</span></button>)}</div><p className="text-sm text-muted">Winner and runner-up certificates recognize teams. Participation recognizes eligible teams or players.</p></div>}
    {step === 1 && <div className="space-y-4">{loading ? <LoadingIndicator label="Checking finalized competition…" /> : <><Field label="Competition / event">{props => <Select {...props} value={data?.approval?.id || ""} disabled={!data?.approval}><option value="">No approved competition available</option>{data?.approval && <option value={data.approval.id}>{data.approval.championship} · {data.approval.sport}</option>}</Select>}</Field><p className="text-sm text-muted">{data?.guidance}</p>{data?.approval && <p className="text-xs text-muted">Approved by {data.approval.approvedBy} · {new Date(data.approval.approvedAt).toLocaleString()}</p>}</>}</div>}
    {step === 2 && <div className="space-y-4"><Field label="Recipient type">{props => <Select {...props} value={type} disabled={category !== "PARTICIPATION" || !!reissue} onChange={e => { setType(e.target.value as RecipientType); setRecipient(""); }}><option value="TEAM">Team</option>{category === "PARTICIPATION" && <option value="PLAYER">Player</option>}</Select>}</Field>{loading ? <LoadingIndicator label="Finding eligible recipients…" /> : !data?.recipients.length ? <EmptyState title="No eligible recipients" description={data?.guidance} /> : <div className="max-h-72 space-y-2 overflow-y-auto" role="radiogroup" aria-label="Eligible recipients">{data.recipients.map(r => <label key={r.id} className={`flex cursor-pointer items-center gap-3 rounded-xl border p-4 ${recipient === r.id ? "border-primary bg-primary/5" : "border-border"}`}><input type="radio" name="certificate-recipient" value={r.id} checked={recipient === r.id} onChange={() => { setRecipient(r.id); setRequestId(crypto.randomUUID()); setPreview(""); }} /><PlayerIdentity name={r.name} imageUrl={r.photoUrl} metadata={r.team || r.type} /><span className="ml-auto"><StatusBadge tone="success">Eligible</StatusBadge></span></label>)}</div>}{reissue && <Field label="Correction justification" hint="At least 10 characters. A new reference will be linked to the revoked certificate.">{props => <Textarea {...props} maxLength={1000} value={reason} onChange={e => { setReason(e.target.value); setRequestId(crypto.randomUUID()); }} />}</Field>}</div>}
    {step === 3 && <div className="space-y-3"><StatusBadge tone="warning">Preview only · Not issued</StatusBadge>{preview && <PdfPreview url={preview} imageUrl={previewImage} />}</div>}
    {step === 4 && (issued ? <div className="space-y-5 rounded-xl border border-primary/25 bg-primary/5 p-6"><CheckCircle2 className="text-primary" size={36} /><h3 className="section-title">Certificate issued</h3><p>{issued.certificate.recipient.name} · {categoryLabel[issued.certificate.category]}</p><p className="break-all font-mono text-sm">{issued.certificate.reference}</p><Button onClick={() => void saveCertificate(cid, issued).catch(e => toast.error(certificateError(e)))}><Download size={16} />Download PDF</Button></div> : <div className="space-y-4 rounded-xl border border-border p-5"><h3 className="section-title">Ready to recognize {chosen?.name}</h3><p className="text-sm text-muted">{categoryLabel[category]} · {data?.approval?.championship}. The server rechecks eligibility before issuance.</p><Checkbox label="I have reviewed the recipient, award and PDF preview." checked={confirmed} onChange={e => setConfirmed(e.target.checked)} />{busy && <LoadingIndicator label="Validating results and issuing certificate…" />}</div>)}
    <div className="mt-6 flex justify-end gap-2">{issued ? <Button onClick={onClose}>Done</Button> : <><Button variant="ghost" disabled={busy} onClick={() => step ? setStep(step - 1) : onClose()}>{step ? "Back" : "Cancel"}</Button>{step < 2 ? <Button disabled={busy || (step === 1 && (!data?.approval || loading))} onClick={() => setStep(step + 1)}>Continue</Button> : step === 2 ? <Button loading={busy} disabled={!recipient || loading || (!!reissue && reason.trim().length < 10)} onClick={() => void renderPreview()}>Preview certificate</Button> : step === 3 ? <Button onClick={() => setStep(4)}>Continue to generation</Button> : <Button loading={busy} disabled={!confirmed} onClick={() => void generate()}>Issue certificate</Button>}</>}</div>
  </Dialog>;
}
