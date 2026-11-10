import AsyncStorage from '@react-native-async-storage/async-storage';
import type { Session } from '@supabase/supabase-js';
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

import { supabase } from '@/lib/supabase';

type AuthState = { session: Session | null; loading: boolean; userId?: string };

const AuthContext = createContext<AuthState>({ session: null, loading: true });

/**
 * The session Supabase saved on the device. Used when the phone is offline and the
 * access token has expired: Supabase can't refresh it, but the user is still signed in.
 */
async function storedSession(): Promise<Session | null> {
  try {
    const ref = new URL(process.env.EXPO_PUBLIC_SUPABASE_URL!).hostname.split('.')[0];
    const raw = await AsyncStorage.getItem(`sb-${ref}-auth-token`);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    supabase.auth.getSession().then(async ({ data, error }) => {
      // An error here (not a missing session) means the refresh failed, usually offline.
      setSession(data.session ?? (error ? await storedSession() : null));
      setLoading(false);
    });
    const { data } = supabase.auth.onAuthStateChange((event, s) => {
      // While offline Supabase can report an empty session; only a real sign-out clears it.
      if (!s && event !== 'SIGNED_OUT') return;
      setSession(s);
    });
    return () => data.subscription.unsubscribe();
  }, []);

  return (
    <AuthContext.Provider value={{ session, loading, userId: session?.user.id }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
