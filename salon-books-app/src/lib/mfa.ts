import type { Factor, Session } from '@supabase/supabase-js';

import { supabase } from './supabase';

/** "aal1" = password only, "aal2" = password + authenticator code. */
export function tokenLevel(session: Session | null): string | undefined {
  if (!session) return undefined;
  try {
    const part = session.access_token.split('.')[1];
    const base64 = part.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(part.length / 4) * 4, '=');
    return (JSON.parse(atob(base64)) as { aal?: string }).aal;
  } catch {
    return undefined;
  }
}

/** The user's working authenticator app, if 2FA is on. */
export function verifiedTotp(session: Session | null): Factor | undefined {
  return session?.user.factors?.find((f) => f.factor_type === 'totp' && f.status === 'verified');
}

/** Signed in with a password, but 2FA is on and the code hasn't been entered yet. */
export function needsCode(session: Session | null): boolean {
  return !!verifiedTotp(session) && tokenLevel(session) !== 'aal2';
}

/** Supabase's wording is technical; turn the common cases into something a stylist can act on. */
export function codeError(message: string): string {
  const m = message.toLowerCase();
  if (m.includes('invalid') || m.includes('code')) {
    return "That code didn't work. Enter the newest code from your authenticator app.";
  }
  if (m.includes('rate') || m.includes('too many')) return 'Too many tries. Wait a minute and try again.';
  if (m.includes('fetch') || m.includes('network')) return "Can't connect right now. Please try again later.";
  return message;
}

/** Checks a 6-digit code against the user's authenticator and upgrades the session. */
export async function verifyCode(factorId: string, code: string): Promise<void> {
  const { error } = await supabase.auth.mfa.challengeAndVerify({ factorId, code });
  if (error) throw new Error(codeError(error.message));
}
