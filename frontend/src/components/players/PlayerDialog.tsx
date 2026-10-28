import { useState } from "react";
import { api, PlayerDto } from "@/lib/api";
import { toast } from "sonner";
import { Dialog } from "@/components/ui/Dialog";

interface Props {
  player?: PlayerDto | null;
  defaultBasePrice?: number;
  onClose: () => void;
  onSaved: () => void;
}

export default function PlayerDialog({ player, defaultBasePrice = 2000000, onClose, onSaved }: Props) {
  const [formData, setFormData] = useState({
    fullName: player?.fullName || "",
    gender: player?.gender || "MALE",
    basePrice: player?.basePrice ? Number(player.basePrice) : defaultBasePrice,
    skillLevel: player?.skillLevel || "Intermediate",
    auctionOrder: player?.auctionOrder || "",
    playerReference: player?.playerReference || "",
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const payload = {
        ...formData,
        auctionOrder: formData.auctionOrder ? Number(formData.auctionOrder) : null,
      };
      if (player) {
        await api.put(`/admin/players/${player.id}`, payload);
        toast.success("Player updated");
      } else {
        await api.post("/admin/players", payload);
        toast.success("Player created");
      }
      onSaved();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || err?.response?.data || "Failed to save player");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog title={player ? "Edit Player" : "Add Player"} onClose={onClose}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="player-field-0" className="field-label">Full Name</label>
            <input
              id="player-field-0"
              required
              className="input"
              value={formData.fullName}
              onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="player-field-1" className="field-label">Gender</label>
              <select
              id="player-field-1"
                className="input"
                value={formData.gender}
                onChange={(e) => setFormData({ ...formData, gender: e.target.value as "MALE" | "FEMALE" })}
              >
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
              </select>
            </div>
            <div>
              <label htmlFor="player-field-2" className="field-label">Skill Level</label>
              <select
              id="player-field-2"
                className="input"
                value={formData.skillLevel}
                onChange={(e) => setFormData({ ...formData, skillLevel: e.target.value })}
              >
                <option value="Elite">Elite</option>
                <option value="Pro">Pro</option>
                <option value="Advanced">Advanced</option>
                <option value="Intermediate">Intermediate</option>
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="player-field-3" className="field-label">Base Price (₹)</label>
              <input
              id="player-field-3"
                required
                type="number"
                step="0.01"
                min="0.01"
                className="input"
                value={formData.basePrice}
                onChange={(e) => setFormData({ ...formData, basePrice: Number(e.target.value) })}
              />
            </div>
            <div>
              <label htmlFor="player-field-4" className="field-label">Auction Order</label>
              <input
              id="player-field-4"
                type="number"
                placeholder="Auto"
                className="input"
                value={formData.auctionOrder}
                onChange={(e) => setFormData({ ...formData, auctionOrder: e.target.value })}
              />
            </div>
          </div>
          <div><label htmlFor="player-reference" className="field-label">Player Reference</label><input id="player-reference" className="input" maxLength={64} pattern="[A-Za-z0-9][A-Za-z0-9_-]{0,63}" value={formData.playerReference} disabled={!!player?.playerReference} onChange={e=>setFormData({...formData,playerReference:e.target.value})}/><p className="mt-1 text-xs text-muted">Optional stable identifier for safe repeat imports. Once assigned, it stays with this player.</p></div>
          <div className="flex justify-end gap-3 mt-6">
            <button type="button" onClick={onClose} className="btn btn-ghost">Cancel</button>
            <button type="submit" disabled={loading} className="btn btn-primary">
              {loading ? "Saving..." : "Save"}
            </button>
          </div>
        </form>
    </Dialog>
  );
}
