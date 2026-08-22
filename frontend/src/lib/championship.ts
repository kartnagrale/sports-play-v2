"use client";

import { create } from "zustand";
import { api, ChampionshipDto } from "./api";

const CHAMPIONSHIP_KEY = "sports_active_championship";
const AUCTION_KEY = "sports_active_auction";
export const VIEWER_TOKEN_KEY = "sports_viewer_token";
export const VIEWER_CHAMPIONSHIP_KEY = "sports_viewer_championship";

interface ChampionshipState {
  championships: ChampionshipDto[];
  active: ChampionshipDto | null;
  hydrated: boolean;
  load: () => Promise<void>;
  select: (championship: ChampionshipDto) => void;
  setViewer: (championship: ChampionshipDto, token: string) => void;
  clearViewer: () => void;
}

export function activeTenant() {
  if (typeof window === "undefined") return { championshipId: null, auctionId: null };
  return { championshipId: sessionStorage.getItem(CHAMPIONSHIP_KEY), auctionId: sessionStorage.getItem(AUCTION_KEY) };
}

export const useChampionship = create<ChampionshipState>((set, get) => ({
  championships: [], active: null, hydrated: false,
  load: async () => {
    const viewerRaw = sessionStorage.getItem(VIEWER_CHAMPIONSHIP_KEY);
    if (viewerRaw && sessionStorage.getItem(VIEWER_TOKEN_KEY)) {
      const viewer = JSON.parse(viewerRaw) as ChampionshipDto;
      get().select(viewer); set({ championships: [viewer], active: viewer, hydrated: true }); return;
    }
    try {
      const { data } = await api.get<ChampionshipDto[]>("/championships");
      const saved = sessionStorage.getItem(CHAMPIONSHIP_KEY);
      const selected = data.find((c) => c.id === saved) || data[0] || null;
      if (selected) get().select(selected);
      set({ championships: data, active: selected, hydrated: true });
    } catch { set({ championships: [], active: null, hydrated: true }); }
  },
  select: (championship) => {
    sessionStorage.setItem(CHAMPIONSHIP_KEY, championship.id);
    if (championship.defaultAuctionId) sessionStorage.setItem(AUCTION_KEY, championship.defaultAuctionId);
    else sessionStorage.removeItem(AUCTION_KEY);
    set({ active: championship });
  },
  setViewer: (championship, token) => {
    sessionStorage.setItem(VIEWER_TOKEN_KEY, token);
    sessionStorage.setItem(VIEWER_CHAMPIONSHIP_KEY, JSON.stringify(championship));
    get().select(championship); set({ championships: [championship], active: championship, hydrated: true });
  },
  clearViewer: () => { sessionStorage.removeItem(VIEWER_TOKEN_KEY); sessionStorage.removeItem(VIEWER_CHAMPIONSHIP_KEY); },
}));
