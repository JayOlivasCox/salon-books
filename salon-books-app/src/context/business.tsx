import AsyncStorage from '@react-native-async-storage/async-storage';
import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';

import { api } from '@/lib/api';
import type { Business, Category, Client, ServiceType } from '@/lib/types';
import { useAuth } from './auth';

const SELECTED_KEY = 'selectedBusinessId';

type BusinessState = {
  loading: boolean;
  error?: string;
  businesses: Business[];
  business?: Business;
  /** The signed-in stylist's user id (used as stylistId). */
  me?: string;
  categories: Category[];
  services: ServiceType[];
  clients: Client[];
  select: (id: string) => void;
  refresh: () => Promise<void>;
  refreshLists: () => Promise<void>;
  setBusiness: (b: Business) => void;
};

const BusinessContext = createContext<BusinessState>({
  loading: true, businesses: [], categories: [], services: [], clients: [],
  select: () => {}, refresh: async () => {}, refreshLists: async () => {}, setBusiness: () => {},
});

async function fetchBusinesses() {
  const [me, saved] = await Promise.all([api.me(), AsyncStorage.getItem(SELECTED_KEY)]);
  return { businesses: me.businesses, saved: saved ?? undefined };
}

async function fetchLists(businessId: string) {
  const [categories, services, clients] = await Promise.all([
    api.categories(businessId), api.services(businessId), api.clients(businessId),
  ]);
  return { categories, services, clients };
}

export function BusinessProvider({ children }: { children: ReactNode }) {
  const { userId } = useAuth();
  // Which user the business list was loaded for; while it doesn't match, we're loading.
  const [loadedFor, setLoadedFor] = useState<string | null | undefined>(undefined);
  const [error, setError] = useState<string>();
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [selectedId, setSelectedId] = useState<string>();
  const [categories, setCategories] = useState<Category[]>([]);
  const [services, setServices] = useState<ServiceType[]>([]);
  const [clients, setClients] = useState<Client[]>([]);

  const business = businesses.find((b) => b.id === selectedId) ?? businesses[0];
  const businessId = business?.id;
  const loading = loadedFor !== (userId ?? null);

  // Load the user's businesses whenever the signed-in user changes.
  useEffect(() => {
    let live = true;
    (async () => {
      try {
        const result = userId ? await fetchBusinesses() : { businesses: [], saved: undefined };
        if (!live) return;
        setBusinesses(result.businesses);
        setSelectedId(result.saved);
        setError(undefined);
      } catch (e) {
        if (live) setError(e instanceof Error ? e.message : String(e));
      } finally {
        if (live) setLoadedFor(userId ?? null);
      }
    })();
    return () => { live = false; };
  }, [userId]);

  // Load pickers (categories, services, clients) for the current business.
  useEffect(() => {
    if (!businessId) return;
    let live = true;
    fetchLists(businessId).then((l) => {
      if (!live) return;
      setCategories(l.categories);
      setServices(l.services);
      setClients(l.clients);
    }).catch(() => {});
    return () => { live = false; };
  }, [businessId]);

  const refresh = useCallback(async () => {
    if (!userId) return;
    try {
      const result = await fetchBusinesses();
      setBusinesses(result.businesses);
      setSelectedId(result.saved);
      setError(undefined);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }, [userId]);

  const refreshLists = useCallback(async () => {
    if (!businessId) return;
    const l = await fetchLists(businessId);
    setCategories(l.categories);
    setServices(l.services);
    setClients(l.clients);
  }, [businessId]);

  const select = (id: string) => {
    setSelectedId(id);
    AsyncStorage.setItem(SELECTED_KEY, id);
  };

  const setBusiness = (b: Business) => {
    setBusinesses((prev) => (prev.some((x) => x.id === b.id) ? prev.map((x) => (x.id === b.id ? b : x)) : [...prev, b]));
    select(b.id);
  };

  return (
    <BusinessContext.Provider value={{
      loading, error, businesses, business, me: userId, categories, services, clients,
      select, refresh, refreshLists, setBusiness,
    }}>
      {children}
    </BusinessContext.Provider>
  );
}

export const useBusiness = () => useContext(BusinessContext);

const PLACEHOLDER: Business = {
  id: '', name: '', timezone: 'America/Los_Angeles', rentDisplay: 'MONTHLY', defaultExpenseSort: 'DATE', role: 'PARTNER',
};

/**
 * For screens that only render once a business exists. During sign-out a screen can render
 * once more before it unmounts, so this falls back to a placeholder instead of crashing.
 */
export function useCurrentBusiness() {
  const ctx = useBusiness();
  return { ...ctx, business: ctx.business ?? PLACEHOLDER, me: ctx.me ?? '' };
}
