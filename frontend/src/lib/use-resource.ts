"use client";
import { useCallback, useEffect, useState } from "react";
import { api } from "./api";
import { useChampionship } from "./championship";

/** Scoped reads with cancellation, explicit pending/error states and retry. */
export function useResource<T>(path: string | null) {
  const [revision, setRevision] = useState(0);
  const key = `${path}:${revision}`;
  const [result, setResult] = useState<{ key: string; path?: string; data?: T; error?: string }>({ key: "" });
  useEffect(() => {
    if (!path) return;
    const controller = new AbortController();
    api.get<T>(path, { signal: controller.signal }).then(({ data }) => { if (!controller.signal.aborted) setResult({ key, path, data }); })
      .catch(error => { if (!controller.signal.aborted) setResult({ key, path, error: error?.response?.data?.message || "Could not load this view. Please try again." }); });
    return () => controller.abort();
  }, [path, key]);
  const refresh = useCallback(() => setRevision(value => value + 1), []);
  return { data: path && result.path === path ? result.data : undefined, error: path && result.key === key ? result.error : undefined, loading: !!path && result.key !== key && (result.path !== path || result.data === undefined), refreshing: !!path && result.key !== key, refresh };
}
export function useTenantResource<T>(path: string | null) {
  const id = useChampionship(state => state.active?.id);
  return useResource<T>(id && path ? `/championships/${id}${path}` : null);
}
