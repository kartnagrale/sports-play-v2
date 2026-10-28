/** Coalesce event bursts; serialize refreshes and retain one trailing refresh while a read is running. */
export function createRefreshQueue(refresh: () => Promise<unknown> | unknown, delay = 150) {
  let timer: ReturnType<typeof setTimeout> | undefined, running = false, pending = false, disposed = false;
  const run = async () => {
    timer = undefined;
    if (disposed || running) return;
    running = true; pending = false;
    try { await refresh(); } catch { /* Resource hooks surface their own request error. */ }
    finally { running = false; if (pending && !disposed) timer = setTimeout(() => void run(), delay); }
  };
  return {
    request() { if (disposed) return; pending = true; if (!timer && !running) timer = setTimeout(() => void run(), delay); },
    dispose() { disposed = true; pending = false; clearTimeout(timer); },
  };
}
