"use client";

import { useEffect, useState } from "react";
import { api, PlayerDto } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { formatCr } from "@/lib/format";
import { toast } from "sonner";
import { Plus, Pencil, Trash2 } from "lucide-react";
import PlayerDialog from "@/components/players/PlayerDialog";

export default function PlayersPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const [players, setPlayers] = useState<PlayerDto[]>([]);
  const [loading, setLoading] = useState(true);
  
  // Dialog state
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingPlayer, setEditingPlayer] = useState<PlayerDto | null>(null);

  const loadPlayers = async () => {
    try {
      const res = await api.get<PlayerDto[]>("/players");
      setPlayers(res.data);
    } catch (e) {
      console.error(e);
      toast.error("Failed to load players");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPlayers();
  }, []);

  const handleDelete = async (player: PlayerDto) => {
    if (!window.confirm(`Are you sure you want to delete ${player.fullName}?`)) return;
    try {
      await api.delete(`/admin/players/${player.id}`);
      toast.success("Player deleted");
      loadPlayers();
    } catch (e: any) {
      toast.error(e?.response?.data?.message || "Delete failed");
    }
  };

  const openAdd = () => {
    setEditingPlayer(null);
    setIsDialogOpen(true);
  };

  const openEdit = (p: PlayerDto) => {
    setEditingPlayer(p);
    setIsDialogOpen(true);
  };

  if (loading) {
    return <div className="p-10 text-white/40 uppercase tracking-widest h-heading">Loading...</div>;
  }

  return (
    <div className="p-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <div className="label-cap">Player Management</div>
          <h1 className="h-heading text-5xl font-bold mt-1">All Players</h1>
        </div>
        {isAdmin && (
          <button onClick={openAdd} className="btn btn-primary">
            <Plus size={16} /> Add Player
          </button>
        )}
      </div>

      <div className="card-elev rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/10 text-white/40 label-cap text-xs">
                <th className="p-4 font-normal">Order</th>
                <th className="p-4 font-normal">Name</th>
                <th className="p-4 font-normal">Gender</th>
                <th className="p-4 font-normal">Skill</th>
                <th className="p-4 font-normal">Base Price</th>
                <th className="p-4 font-normal">Status</th>
                <th className="p-4 font-normal">Team</th>
                {isAdmin && <th className="p-4 font-normal text-right">Actions</th>}
              </tr>
            </thead>
            <tbody>
              {players.map((p) => (
                <tr key={p.id} className="border-b border-white/5 hover:bg-white/5 transition-colors group">
                  <td className="p-4 text-white/40">{p.auctionOrder}</td>
                  <td className="p-4 font-medium">{p.fullName}</td>
                  <td className="p-4">
                    <span className="chip">{p.gender}</span>
                  </td>
                  <td className="p-4 text-white/60">{p.skillLevel}</td>
                  <td className="p-4 text-primary font-mono">{formatCr(Number(p.basePrice))}</td>
                  <td className="p-4">
                    <span className={`chip ${p.status === 'SOLD' ? 'chip-primary' : ''}`}>
                      {p.status}
                    </span>
                  </td>
                  <td className="p-4 text-white/60">
                    {p.teamName || "—"}
                  </td>
                  {isAdmin && (
                    <td className="p-4 text-right">
                      <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button onClick={() => openEdit(p)} className="p-2 text-white/40 hover:text-white rounded hover:bg-white/10 transition-colors" title="Edit">
                          <Pencil size={14} />
                        </button>
                        <button onClick={() => handleDelete(p)} className="p-2 text-white/40 hover:text-danger rounded hover:bg-white/10 transition-colors" title="Delete">
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
              {players.length === 0 && (
                <tr>
                  <td colSpan={isAdmin ? 8 : 7} className="p-8 text-center text-white/40">
                    No players found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {isDialogOpen && (
        <PlayerDialog 
          player={editingPlayer} 
          onClose={() => setIsDialogOpen(false)} 
          onSaved={() => {
            setIsDialogOpen(false);
            loadPlayers();
          }} 
        />
      )}
    </div>
  );
}
