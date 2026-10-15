import { useFocusEffect } from 'expo-router';
import { useCallback, useState, useSyncExternalStore } from 'react';

import { dayCache } from './day-cache';

/**
 * One day's report, served from the cache when possible.
 * Refetches on focus only if the cached copy is older than a few minutes.
 */
export function useDay(businessId: string, day: string) {
  const key = `${businessId}|${day}`;
  const entry = useSyncExternalStore(dayCache.subscribe, () => dayCache.get(businessId, day));
  const [error, setError] = useState<{ key: string; message: string }>();
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(
    (force: boolean) =>
      dayCache.load(businessId, day, force)
        .then(() => setError(undefined))
        .catch((e: unknown) => setError({ key, message: e instanceof Error ? e.message : String(e) })),
    [businessId, day, key],
  );

  // Runs on focus and whenever the day changes.
  useFocusEffect(
    useCallback(() => {
      if (businessId) load(false);
    }, [businessId, load]),
  );

  const refresh = useCallback(async () => {
    setRefreshing(true);
    await load(true);
    setRefreshing(false);
  }, [load]);

  return {
    data: entry?.data,
    loading: !entry,
    error: error?.key === key ? error.message : undefined,
    /** Force a fresh copy (after saving, or pull-to-refresh). */
    reload: () => load(true),
    refresh,
    refreshing,
  };
}
