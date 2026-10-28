import type { AuctionStateDto, BidDto, TeamDto } from "./api";
export interface ConfirmedSale { saleId: string; playerId: string; teamId: string; player: string; teamName: string; amount: number; revision: number; photoUrl?:string|null }
export function rankTeams(state: AuctionStateDto): { team: TeamDto; bid?: BidDto; rank?: number }[] {
  const bids = new Map<string, BidDto>();
  for (const bid of state.teamBids || []) {
    if (!bid.active || bid.playerId !== state.currentPlayer?.id) continue;
    const previous = bids.get(bid.teamId);
    if (!previous || Number(bid.amount) > Number(previous.amount)) bids.set(bid.teamId, bid);
  }
  return [...state.teams].sort((a,b) => Number(bids.get(b.id)?.amount || 0) - Number(bids.get(a.id)?.amount || 0) || a.name.localeCompare(b.name) || a.id.localeCompare(b.id)).map((team,i) => ({ team, bid: bids.get(team.id), rank: bids.has(team.id) ? i+1 : undefined }));
}
/** A connection-local reconciler: no event replay or fabricated optimistic bids. */
export class AuctionReconciler {
  snapshot: AuctionStateDto | null = null;
  ready = false;
  previousPlayer: { id: string; revision: number } | null = null;
  private seen = new Set<string>();
  disconnect() { this.ready = false; this.previousPlayer = null; }
  synchronize(next: AuctionStateDto) { this.snapshot = next; this.previousPlayer = null; this.ready = true; }
  accept(next: AuctionStateDto) {
    if (this.snapshot && next.revision < this.snapshot.revision) return false;
    if (this.snapshot?.currentPlayer && next.currentPlayer?.id !== this.snapshot.currentPlayer.id && next.revision > this.snapshot.revision) this.previousPlayer = { id: this.snapshot.currentPlayer.id, revision: next.revision };
    this.snapshot = next; return true;
  }
  matchesResult(playerId: string, revision: number) {
    return this.ready && this.snapshot && revision >= this.snapshot.revision && (this.snapshot.currentPlayer?.id === playerId || (this.previousPlayer?.id === playerId && this.previousPlayer.revision === revision));
  }
  sale(value: ConfirmedSale) {
    if (!value.saleId || !Number.isFinite(Number(value.amount)) || Number(value.amount) <= 0 || !this.matchesResult(value.playerId, value.revision) || this.seen.has(value.saleId)) return false;
    this.seen.add(value.saleId); if (this.seen.size > 256) this.seen.delete(this.seen.values().next().value!);
    return true;
  }
}
