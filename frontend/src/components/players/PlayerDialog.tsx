import { useState, useEffect } from "react";
import { api, PlayerDto } from "@/lib/api";
import { toast } from "sonner";
import { X } from "lucide-react";

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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-bg-elev border border-white/10 rounded-2xl p-6 w-full max-w-md shadow-2xl">
        <div className="flex items-center justify-between mb-6">
          <h2 className="h-heading text-xl">{player ? "Edit Player" : "Add Player"}</h2>
          <button onClick={onClose} className="text-white/40 hover:text-white">
            <X size={20} />
          </button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="label-cap block mb-2">Full Name</label>
            <input
              required
              className="input"
              value={formData.fullName}
              onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label-cap block mb-2">Gender</label>
              <select
                className="input"
                value={formData.gender}
                onChange={(e) => setFormData({ ...formData, gender: e.target.value as "MALE" | "FEMALE" })}
              >
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
              </select>
            </div>
            <div>
              <label className="label-cap block mb-2">Skill Level</label>
              <select
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
              <label className="label-cap block mb-2">Base Price (₹)</label>
              <input
                required
                type="number"
                step="100000"
                className="input"
                value={formData.basePrice}
                onChange={(e) => setFormData({ ...formData, basePrice: Number(e.target.value) })}
              />
            </div>
            <div>
              <label className="label-cap block mb-2">Auction Order</label>
              <input
                type="number"
                placeholder="Auto"
                className="input"
                value={formData.auctionOrder}
                onChange={(e) => setFormData({ ...formData, auctionOrder: e.target.value })}
              />
            </div>
          </div>
          <div className="flex justify-end gap-3 mt-6">
            <button type="button" onClick={onClose} className="btn btn-ghost">Cancel</button>
            <button type="submit" disabled={loading} className="btn btn-primary">
              {loading ? "Saving..." : "Save"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
