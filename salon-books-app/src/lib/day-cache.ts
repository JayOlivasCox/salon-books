import { api } from './api';
import { addDays, isoDate } from './format';
import { persist } from './persist';
import type { DailyReport } from './types';

/** A cached day is shown without refetching for this long. */
const FRESH_MS = 5 * 60_000;
/** Days kept in memory; the device cache keeps a week of history beyond that. */
const MAX_IN_MEMORY = 60;
/** How many days back to preload when the app opens (plus today). */
const PREFETCH_DAYS = 7;

/** at = when this copy came from the server. */
export type DayEntry = { data: DailyReport; at: number };

const entries = new Map<string, DayEntry>();
const inflight = new Map<string, Promise<DailyReport>>();
const listeners = new Set<() => void>();

const keyOf = (businessId: string, date: string) => `${businessId}|${date}`;
const storeKey = (businessId: string, date: string) => `day:${businessId}:${date}`;

function emit() {
  listeners.forEach((l) => l());
}

function put(key: string, entry: DayEntry) {
  entries.set(key, entry);
  if (entries.size > MAX_IN_MEMORY) {
    let oldestKey: string | undefined;
    let oldestAt = Infinity;
    for (const [k, e] of entries) {
      if (e.at < oldestAt) {
        oldestAt = e.at;
        oldestKey = k;
      }
    }
    if (oldestKey) entries.delete(oldestKey);
  }
  emit();
}

/** Pull a saved copy from the device into memory, if there is one. */
async function hydrate(businessId: string, date: string) {
  const key = keyOf(businessId, date);
  if (entries.has(key)) return;
  const saved = await persist.read<DailyReport>(storeKey(businessId, date));
  if (saved && !entries.has(key)) put(key, { data: saved.data, at: saved.savedAt });
}

/**
 * Daily reports, cached in memory and on the device. Screens read it through
 * useSyncExternalStore, so a cached day renders instantly and updates when a
 * fresh copy arrives.
 */
export const dayCache = {
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },

  get(businessId: string, date: string): DayEntry | undefined {
    return entries.get(keyOf(businessId, date));
  },

  /** Returns the cached day if it's fresh, otherwise fetches it. force always fetches. */
  load(businessId: string, date: string, force = false): Promise<DailyReport> {
    const key = keyOf(businessId, date);
    const cached = entries.get(key);
    if (!force && cached && Date.now() - cached.at < FRESH_MS) return Promise.resolve(cached.data);
    const running = inflight.get(key);
    if (!force && running) return running;

    const p = hydrate(businessId, date)
      .then(() => api.daily(businessId, date))
      .then((data) => {
        put(key, { data, at: Date.now() });
        persist.write(storeKey(businessId, date), data);
        return data;
      })
      .finally(() => {
        if (inflight.get(key) === p) inflight.delete(key);
      });
    inflight.set(key, p);
    return p;
  },

  /** Preload today and the past week in the background. Failures are ignored. */
  prefetchWeek(businessId: string, today = new Date()) {
    for (let i = 0; i <= PREFETCH_DAYS; i++) {
      dayCache.load(businessId, isoDate(addDays(today, -i))).catch(() => {});
    }
  },

  clear() {
    entries.clear();
    inflight.clear();
    emit();
  },
};
