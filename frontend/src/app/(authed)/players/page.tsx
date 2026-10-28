"use client";

import { useCallback, useEffect, useState } from "react";
import { api, PlayerDto } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { formatCr } from "@/lib/format";
import { toast } from "sonner";
import { Plus, Pencil, Trash2, Camera } from "lucide-react";
import { PlayerIdentity } from "@/components/players/PlayerAvatar";
import { DataTable, PageHeader, LoadingIndicator, SearchInput, Pagination, Select, ErrorState } from "@/components/ui";
import PlayerDialog from "@/components/players/PlayerDialog";
import { useChampionship } from "@/lib/championship";
import PlayerImportDialog, { downloadPlayerTemplate } from "@/components/players/PlayerImportDialog";
import PlayerPhotoDialog from "@/components/players/PlayerPhotoDialog";

export default function PlayersPage() {
  const { user } = useAuth();
  const active = useChampionship((state) => state.active);
  const championshipId=active?.id;
  const championshipRole = user?.championships.find((entry) => entry.championshipId === active?.id)?.role;
  const isAdmin = championshipRole === "CHAMPIONSHIP_ADMIN";
  const [defaultBasePrice, setDefaultBasePrice] = useState(2000000);
  const [players, setPlayers] = useState<PlayerDto[]>([]);
  const [loading, setLoading] = useState(true);

  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [status, setStatus] = useState("ALL");
  const [sort, setSort] = useState("name");
  const [withoutPhotos,setWithoutPhotos]=useState(false);
  const [page, setPage] = useState(1);
  const filtered = players.filter(player => (!withoutPhotos||!player.photoUrl)&&player.fullName.toLocaleLowerCase().includes(search.toLocaleLowerCase()) && (status === "ALL" || player.status === status)).sort((a,b) => sort === "price" ? Number(b.basePrice) - Number(a.basePrice) || a.fullName.localeCompare(b.fullName) : a.fullName.localeCompare(b.fullName));
  const pageCount = Math.max(1, Math.ceil(filtered.length / 20));
  const currentPage = Math.min(page, pageCount);
  const visiblePlayers = filtered.slice((currentPage - 1) * 20, currentPage * 20);

  // Dialog state
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingPlayer, setEditingPlayer] = useState<PlayerDto | null>(null);
  const [importOpen,setImportOpen]=useState(false);
  const [photoPlayer,setPhotoPlayer]=useState<PlayerDto|null>(null);

  const loadPlayers = useCallback( (signal?:AbortSignal) => {
    if(!championshipId)return;
    return api.get<PlayerDto[]>(`/championships/${championshipId}/players`,{signal}).then((res) => {
if(!signal?.aborted&&useChampionship.getState().active?.id===championshipId){setPlayers(res.data);setError("");}

}).catch((e) => {

      if(!signal?.aborted)setError("Could not load players. Please try again.");

}).finally(() => {
      if(useChampionship.getState().active?.id===championshipId)setLoading(false);
    });
  },[championshipId]);

  useEffect(() => {
    const controller=new AbortController();
    loadPlayers(controller.signal);
    if (isAdmin && championshipId) {
      api.get<{ playerBasePrice: number }>(`/championships/${championshipId}/settings`,{signal:controller.signal})
        .then(({ data }) => {if(!controller.signal.aborted)setDefaultBasePrice(Number(data.playerBasePrice));})
        .catch(() => undefined);
    }
    return ()=>controller.abort();
  }, [championshipId, isAdmin,loadPlayers]);

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

  if (error) return <ErrorState description={error} onRetry={() => loadPlayers()} />;
  if (loading) {
    return <LoadingIndicator label="Loading players…" />;
  }

  return (
    <div className="portal-page">
      <PageHeader title="All Players" eyebrow="Player management" description={`${players.length} registered players`} actions={isAdmin && <div className="flex flex-wrap gap-2"><button onClick={()=>championshipId&&downloadPlayerTemplate(championshipId).catch(()=>toast.error("Template download failed"))} className="btn btn-secondary">Download Excel Template</button><button onClick={()=>setImportOpen(true)} className="btn btn-secondary">Import Players</button><button onClick={openAdd} className="btn btn-primary"><Plus size={16} />Add Player</button></div>} />
      <div className="portal-toolbar"><SearchInput label="Search players" placeholder="Search by player name…" value={search} onChange={event => { setSearch(event.target.value); setPage(1); }} /><Select aria-label="Filter player status" value={status} onChange={e => {setStatus(e.target.value);setPage(1);}}>{["ALL","AVAILABLE","ON_BLOCK","SOLD","UNSOLD"].map(s => <option key={s} value={s}>{s === "ALL" ? "All statuses" : s.replaceAll("_"," ")}</option>)}</Select><Select aria-label="Sort players" value={sort} onChange={e => setSort(e.target.value)}><option value="name">Name A–Z</option><option value="price">Highest base price</option></Select></div>
      <div className="card-elev rounded-2xl overflow-hidden">
        <label className="flex min-h-11 items-center gap-3 p-4 text-sm"><input type="checkbox" checked={withoutPhotos} onChange={e=>{setWithoutPhotos(e.target.checked);setPage(1);}}/>Players without photographs</label>
        <DataTable caption="Championship players">
            <thead>
              <tr className="border-b border-white/10 text-white/40 label-cap text-xs">
                <th scope="col" className="p-4 font-normal">Order</th>
                <th scope="col" className="p-4 font-normal">Name</th>
                <th scope="col" className="p-4 font-normal">Gender</th>
                <th scope="col" className="p-4 font-normal">Skill</th>
                <th scope="col" className="p-4 font-normal">Base Price</th>
                <th scope="col" className="p-4 font-normal">Status</th>
                <th scope="col" className="p-4 font-normal">Team</th>
                {isAdmin && <th scope="col" className="p-4 font-normal text-right">Actions</th>}
              </tr>
            </thead>
            <tbody>
              {visiblePlayers.map((p) => (
                <tr key={p.id} className="border-b border-white/5 hover:bg-white/5 transition-colors group">
                  <td className="p-4 text-white/40">{p.auctionOrder}</td>
                  <td className="p-4 font-medium"><PlayerIdentity name={p.fullName} imageUrl={p.photoUrl} metadata={p.playerReference} /></td>
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
                      <div className="flex items-center justify-end gap-2 transition-colors">
                        <button onClick={()=>setPhotoPlayer(p)} className="icon-button text-primary" aria-label={`${p.photoUrl?"Change":"Add"} Photo for ${p.fullName}`} title={p.photoUrl?"Change Photo":"Add Photo"}><Camera size={18}/></button>
                        <button onClick={() => openEdit(p)} className="p-2 text-white/40 hover:text-white rounded hover:bg-white/10 transition-colors" aria-label={`Edit ${p.fullName}`} title="Edit">
                          <Pencil size={14} />
                        </button>
                        <button onClick={() => handleDelete(p)} className="p-2 text-white/40 hover:text-danger rounded hover:bg-white/10 transition-colors" aria-label={`Delete ${p.fullName}`} title="Delete">
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={isAdmin ? 8 : 7} className="p-8 text-center text-white/40">
                    No players found
                  </td>
                </tr>
              )}
            </tbody>
          </DataTable>
        {pageCount > 1 && <Pagination page={currentPage} pageCount={pageCount} onPageChange={setPage} />}
      </div>

      {importOpen&&championshipId&&<PlayerImportDialog key={championshipId} championshipId={championshipId} championshipName={active.name} onClose={()=>setImportOpen(false)} onSaved={()=>loadPlayers()} />}
      {photoPlayer&&championshipId&&<PlayerPhotoDialog key={`${championshipId}:${photoPlayer.id}`} championshipId={championshipId} player={photoPlayer} onClose={()=>setPhotoPlayer(null)} onSaved={()=>{setPhotoPlayer(null);loadPlayers();toast.success("Photograph updated");}}/>}
      {isDialogOpen && (
        <PlayerDialog
          player={editingPlayer}
          defaultBasePrice={defaultBasePrice}
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
