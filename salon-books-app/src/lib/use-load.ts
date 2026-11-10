import { useFocusEffect } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';

import { isOffline, persist } from './persist';

type Result<T> = { key: string; data?: T; error?: string; offline?: boolean; savedAt?: number };

/**
 * Loads data when the screen gains focus and whenever deps change.
 *
 * - Switching periods (new deps) hides the old data so screens show a spinner.
 * - With `cache`, the last copy is saved on the device for a week: it shows
 *   instantly on reopen while fresh data loads, and stays visible when offline.
 */
export function useLoad<T>(fn: () => Promise<T>, deps: unknown[], options: { cache?: string } = {}) {
  const key = JSON.stringify(deps);
  const cacheKey = options.cache ? `${options.cache}:${key}` : undefined;
  const [result, setResult] = useState<Result<T>>();
  const [loading, setLoading] = useState(true);
  const fnRef = useRef(fn);
  const latestKey = useRef(key);

  useEffect(() => {
    fnRef.current = fn;
    latestKey.current = key;
  });

  const load = useCallback(async () => {
    setLoading(true);

    // Show the saved copy right away (only if nothing is on screen for this period yet).
    if (cacheKey) {
      const saved = await persist.read<T>(cacheKey);
      if (saved && latestKey.current === key) {
        setResult((prev) => (prev?.key === key && prev.data !== undefined
          ? prev
          : { key, data: saved.data, savedAt: saved.savedAt }));
      }
    }

    try {
      const data = await fnRef.current();
      if (cacheKey) persist.write(cacheKey, data);
      // Ignore answers for a period the user already left.
      if (latestKey.current === key) setResult({ key, data });
    } catch (e) {
      if (latestKey.current === key) {
        const offline = isOffline(e);
        const message = e instanceof Error ? e.message : String(e);
        setResult((prev) => {
          const keep = prev?.key === key ? prev : undefined;
          const hasData = keep?.data !== undefined;
          return {
            key,
            data: keep?.data,
            savedAt: keep?.savedAt,
            offline,
            // Offline with saved data shows the offline note instead of an error.
            error: offline && hasData ? undefined : message,
          };
        });
      }
    } finally {
      if (latestKey.current === key) setLoading(false);
    }
  }, [key, cacheKey]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const current = result?.key === key ? result : undefined;
  return {
    data: current?.data,
    error: current?.error,
    loading: loading || !current,
    /** Showing saved data because the server couldn't be reached. */
    offline: !!current?.offline && current.data !== undefined,
    /** When the shown data was saved, if it came from the device cache. */
    savedAt: current?.savedAt,
    reload: load,
  };
}
