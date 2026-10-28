"use client";
import { useEffect, useState } from "react";
import { Download, UsersRound } from "lucide-react";
import { api } from "@/lib/api";
import { categoryLabel, certificateBase, certificateError, saveBlob, type CertificateCategory, type CertificateEligibility, type IssuedCertificate, type RecipientType } from "@/lib/certificates";
import { Button, Checkbox, EmptyState, Field, LoadingIndicator, Select, StatusBadge } from "@/components/ui";
import { Dialog } from "@/components/ui/Dialog";
import { PlayerIdentity } from "@/components/players/PlayerAvatar";

export function CertificateBulkDialog({ cid, onClose, onIssued }: { cid: string; onClose: () => void; onIssued: () => void }) {
  const [category, setCategory] = useState<CertificateCategory>("PARTICIPATION"); const [type, setType] = useState<RecipientType>("PLAYER");
  const [eligibility, setEligibility] = useState<CertificateEligibility>(); const [selected, setSelected] = useState<string[]>([]);
  const [team, setTeam] = useState(""); const [requestId, setRequestId] = useState(() => crypto.randomUUID());
  const [error, setError] = useState(""); const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false); const [confirmed, setConfirmed] = useState(false);
  const [batch, setBatch] = useState<{ id: string; certificates: IssuedCertificate[] }>();
  useEffect(() => {
    const controller = new AbortController(); const timer = setTimeout(() => {
      setLoading(true); setError(""); api.get<CertificateEligibility>(`${certificateBase(cid)}/eligibility`, { params: { category, recipientType: type }, signal: controller.signal })
        .then(r => { setEligibility(r.data); setLoading(false); }).catch(e => { if (!controller.signal.aborted) { setError(certificateError(e)); setLoading(false); } });
    }, 0);
    return () => { clearTimeout(timer); controller.abort(); };
  }, [cid, category, type]);
  function changeSelection(ids: string[]) { setSelected(ids); setConfirmed(false); setRequestId(crypto.randomUUID()); }
  async function generate() {
    setBusy(true); setError("");
    try { const r = await api.post(`${certificateBase(cid)}/batches`, { requestId, approvalId: eligibility!.approval!.id, category, recipientType: type, recipientIds: selected }, { timeout: 120000 }); setBatch(r.data); onIssued(); }
    catch (e) { setError(certificateError(e)); } finally { setBusy(false); }
  }
  async function download() {
    if (!batch) return; setBusy(true); setError("");
    try { const r = await api.get<Blob>(`${certificateBase(cid)}/batches/${batch.id}/zip`, { responseType: "blob", timeout: 120000 }); saveBlob(r.data, `certificates-${batch.id}.zip`); }
    catch (e) { setError(certificateError(e)); } finally { setBusy(false); }
  }
  const recipients = eligibility?.recipients || []; const visible = recipients.filter(r => !team || r.team === team);
  return <Dialog title="Bulk certificate generation" className="!w-[min(800px,calc(100vw-2rem))] !max-w-4xl" onClose={() => !busy && onClose()} description="Issue up to 25 certificates together, then download their PDFs in one ZIP archive.">
    {error && <p role="alert" className="mb-4 rounded-lg border border-danger/30 p-3 text-sm text-danger">{error}</p>}
    {batch ? <div className="space-y-5"><StatusBadge tone="success">{batch.certificates.length} certificates issued</StatusBadge><p className="text-sm text-muted">The server confirmed every issuance. Each certificate has its own reference and appears in history.</p><ul className="max-h-56 overflow-y-auto space-y-2 text-sm">{batch.certificates.map(c => <li key={c.certificate.id}>{c.certificate.recipient.name}<span className="block break-all font-mono text-xs text-muted">{c.certificate.reference}</span></li>)}</ul><Button loading={busy} onClick={() => void download()}><Download size={16} />Download ZIP</Button><p className="break-all text-xs text-muted">Batch {batch.id}</p></div> : <div className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-2"><Field label="Bulk award category">{props => <Select {...props} value={category} disabled={busy} onChange={e => { const c = e.target.value as CertificateCategory; setCategory(c); setType(c === "PARTICIPATION" ? "PLAYER" : "TEAM"); changeSelection([]); setTeam(""); }}>{Object.entries(categoryLabel).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</Select>}</Field><Field label="Bulk recipient type">{props => <Select {...props} value={type} disabled={busy || category !== "PARTICIPATION"} onChange={e => { setType(e.target.value as RecipientType); changeSelection([]); setTeam(""); }}><option value="TEAM">Team</option>{category === "PARTICIPATION" && <option value="PLAYER">Player</option>}</Select>}</Field></div>
      {type === "PLAYER" && <Field label="Team roster filter">{props => <Select {...props} disabled={busy} value={team} onChange={e => setTeam(e.target.value)}><option value="">All eligible players</option>{Array.from(new Set(recipients.map(r => r.team).filter(Boolean))).map(t => <option key={t} value={t!}>{t}</option>)}</Select>}</Field>}
      {loading ? <LoadingIndicator label="Checking eligible recipients…" /> : !visible.length ? <EmptyState title="No eligible recipients" description={eligibility?.guidance} /> : <><div className="flex flex-wrap items-center justify-between gap-3"><p className="text-sm text-muted">{selected.length} selected · Maximum 25</p><div className="flex gap-2"><Button variant="ghost" disabled={busy} onClick={() => changeSelection(visible.slice(0, 25).map(r => r.id))}>Select up to 25</Button><Button variant="ghost" disabled={busy} onClick={() => changeSelection([])}>Clear</Button></div></div><div className="max-h-64 space-y-2 overflow-y-auto rounded-xl border border-border p-3">{visible.map(r => <Checkbox key={r.id} disabled={busy || (selected.length >= 25 && !selected.includes(r.id))} checked={selected.includes(r.id)} onChange={e => changeSelection(e.target.checked ? [...selected, r.id] : selected.filter(id => id !== r.id))} label={<PlayerIdentity name={r.name} imageUrl={r.photoUrl} metadata={r.team || r.type} />} />)}</div></>}
      <p className="text-xs text-muted">{eligibility?.guidance} Recipients with active certificates need revocation before replacement. If any selection fails validation, this request issues no certificates.</p>
      {selected.length > 0 && <div className="space-y-3 rounded-xl border border-primary/25 bg-primary/5 p-4"><p className="flex items-center gap-2 font-medium"><UsersRound size={18} />{categoryLabel[category]} for {selected.length} {type === "PLAYER" ? "players" : "teams"}</p><p className="text-sm text-muted">{recipients.filter(r => selected.includes(r.id)).map(r => r.name).join(", ")}</p><Checkbox disabled={busy} checked={confirmed} onChange={e => setConfirmed(e.target.checked)} label="I confirm these recipients, the award category and championship." /></div>}
      {busy && <LoadingIndicator label={`Validating and issuing ${selected.length} certificates…`} />}
    </div>}
    <div className="mt-6 flex justify-end gap-2"><Button variant="ghost" disabled={busy} onClick={onClose}>{batch ? "Done" : "Cancel"}</Button>{!batch && <Button loading={busy} disabled={loading || !confirmed || !selected.length || !eligibility?.approval} onClick={() => void generate()}>Issue selected certificates</Button>}</div>
  </Dialog>;
}
