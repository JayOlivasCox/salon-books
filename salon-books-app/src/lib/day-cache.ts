import { api } from './api';
import { addDays, isoDate } from './format';
import type { DailyReport } from './types';

/** A cached day is shown without refetching for this long. */
const FRESH_MS = 5 * 60_000;
/** After this long a cached day is dropped entirely. */
const KEEP_MS = 60 * 60_000;
/** How many days back to preload when the app opens (plus today). */
const PREFETCH_DAYS = 7;

export type DayEntry = { data: DailyReport; at: number };

const entries = new Map<string, DayEntry>();
const inflight = new Map<string, Promise<DailyReport>>();
const listeners = new Set<() => void>();

const keyOf = (businessId: string, date: string) => `${businessId}|${date}`;

function emit() {
  listeners.forEach((l) => l());
}

function prune() {
  const now = Date.now();
  for (const [key, e] of entries) if (now - e.at > KEEP_MS) entries.delete(key);
}

/**
 * In-memory cache of daily reports. Screens read it through useSyncExternalStore,
 * so a cached day renders instantly and updates when a fresh copy arrives.
 */
export const dayCache = {
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },

  get(businessId: string, date: string): DayEntry | undefined {
    const e = entries.get(keyOf(businessId, date));
    return e && Date.now() - e.at <= KEEP_MS ? e : undefined;
  },

  /** Returns the cached day if it's fresh, otherwise fetches it. force always fetches. */
  load(businessId: string, date: string, force = false): Promise<DailyReport> {
    const key = keyOf(businessId, date);
    const cached = entries.get(key);
    if (!force && cached && Date.now() - cached.at < FRESH_MS) return Promise.resolve(cached.data);
    const running = inflight.get(key);
    if (!force && running) return running;

    const p = api.daily(businessId, date)
      .then((data) => {
        entries.set(key, { data, at: Date.now() });
        prune();
        emit();
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
