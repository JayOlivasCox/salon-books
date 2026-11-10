import AsyncStorage from '@react-native-async-storage/async-storage';

import { ApiError } from './api';

const PREFIX = 'cache:v1:';
/** Saved data is dropped once it hasn't been used for a week. */
const MAX_IDLE_MS = 7 * 24 * 60 * 60_000;

export type Saved<T> = { data: T; savedAt: number };
type Stored<T> = { data: T; savedAt: number; usedAt: number };

let scope = 'anon';

/** Keep each signed-in user's saved data separate. */
export function setCacheScope(userId: string | undefined) {
  scope = userId ?? 'anon';
}

const fullKey = (key: string) => `${PREFIX}${scope}:${key}`;
const idle = (s: Stored<unknown>) => Date.now() - s.usedAt > MAX_IDLE_MS;

/** True when a request failed because the phone couldn't reach the server. */
export const isOffline = (e: unknown) => e instanceof ApiError && e.status === 0;

/**
 * Small on-device cache (AsyncStorage) so screens open instantly and still
 * show the last data when the phone is offline.
 */
export const persist = {
  async read<T>(key: string): Promise<Saved<T> | undefined> {
    try {
      const k = fullKey(key);
      const raw = await AsyncStorage.getItem(k);
      if (!raw) return undefined;
      const s = JSON.parse(raw) as Stored<T>;
      if (idle(s)) {
        await AsyncStorage.removeItem(k);
        return undefined;
      }
      // Reading counts as use, so it stays another week.
      AsyncStorage.setItem(k, JSON.stringify({ ...s, usedAt: Date.now() })).catch(() => {});
      return { data: s.data, savedAt: s.savedAt };
    } catch {
      return undefined;
    }
  },

  async write<T>(key: string, data: T): Promise<void> {
    const now = Date.now();
    const stored: Stored<T> = { data, savedAt: now, usedAt: now };
    await AsyncStorage.setItem(fullKey(key), JSON.stringify(stored)).catch(() => {});
  },

  /** Drop everything not used in the last week. Runs once when the app opens. */
  async prune(): Promise<void> {
    try {
      const keys = (await AsyncStorage.getAllKeys()).filter((k) => k.startsWith(PREFIX));
      const pairs = await AsyncStorage.multiGet(keys);
      const stale = pairs
        .filter(([, v]) => {
          try {
            return !v || idle(JSON.parse(v) as Stored<unknown>);
          } catch {
            return true;
          }
        })
        .map(([k]) => k);
      if (stale.length) await AsyncStorage.multiRemove(stale);
    } catch {
      // Cache cleanup is best effort.
    }
  },

  /** Remove one user's saved data (on sign-out). */
  async clearUser(userId: string): Promise<void> {
    try {
      const keys = (await AsyncStorage.getAllKeys()).filter((k) => k.startsWith(`${PREFIX}${userId}:`));
      if (keys.length) await AsyncStorage.multiRemove(keys);
    } catch {
      // Best effort.
    }
  },
};
