"use client";

import { create } from "zustand";
import { api, NavigationResponse, NavigationRole, ScreenDto } from "./api";

interface NavigationState {
  role: NavigationRole | null;
  screens: ScreenDto[];
  loading: boolean;
  loadedFor: string | null;
  load: (championshipId?: string | null) => Promise<void>;
  clear: () => void;
}

export const useNavigation = create<NavigationState>((set) => ({
  role: null,
  screens: [],
  loading: true,
  loadedFor: null,
  load: async (championshipId) => {
    const key = championshipId || "global";
    set({ loading: true, loadedFor: key });
    try {
      const { data } = await api.get<NavigationResponse>("/navigation", {
        params: championshipId ? { championshipId } : undefined,
      });
      set({ role: data.role, screens: data.screens, loading: false, loadedFor: key });
    } catch (error) {
      console.warn("Navigation load failed:", error);
      set({ role: null, screens: [], loading: false, loadedFor: key });
    }
  },
  clear: () => set({ role: null, screens: [], loading: true, loadedFor: null }),
}));
