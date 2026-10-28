import { api } from "./api";
export type CertificateCategory = "WINNER" | "RUNNER_UP" | "PARTICIPATION";
export type RecipientType = "TEAM" | "PLAYER";
export interface CertificateRecipient { type: RecipientType; id: string; name: string; team: string | null; photoUrl: string | null }
export interface CertificateApproval { id: string; championshipId: string; championship: string; sport: string; approvedAt: string; approvedBy: string; justification: string }
export interface CertificateEligibility { approval: CertificateApproval | null; recipients: CertificateRecipient[]; guidance: string }
export interface CertificateSnapshot { id: string; reference: string; championshipId: string; championship: string; sport: string; event: string; approvalId: string; category: CertificateCategory; recipient: CertificateRecipient; achievement: string; issuedAt: string; issuer: string; reissuedFrom: string | null; verificationUrl: string | null }
export interface IssuedCertificate { certificate: CertificateSnapshot; status: "ISSUED" | "REVOKED"; revokedAt: string | null; revocationReason: string | null }
export interface CertificateHistory { items: IssuedCertificate[]; total: number; counts: Record<string, number> }
export interface CertificateIssueRequest { requestId: string; approvalId: string; category: CertificateCategory; recipientType: RecipientType; recipientId: string; reissuedFrom?: string; reason?: string }
export const categoryLabel: Record<CertificateCategory, string> = { WINNER: "Winner", RUNNER_UP: "Runner-up", PARTICIPATION: "Participation" };
export const certificateBase = (cid: string) => `/championships/${cid}/admin/certificates`;
export function certificateError(error: unknown): string {
  if (error && typeof error === "object" && "response" in error) {
    const data = (error as { response?: { data?: { message?: string } } }).response?.data;
    if (data?.message) return data.message;
  }
  return "The request could not be completed. Refresh eligibility or retry with the same selection.";
}
export async function saveCertificate(cid: string, issued: IssuedCertificate) {
  const { data } = await api.get<Blob>(`${certificateBase(cid)}/${issued.certificate.id}/pdf`, { responseType: "blob" });
  saveBlob(data, `${issued.certificate.reference}.pdf`);
}
export function saveBlob(data: Blob, filename: string) {
  const url = URL.createObjectURL(data); const a = document.createElement("a");
  a.href = url; a.download = filename; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
}
