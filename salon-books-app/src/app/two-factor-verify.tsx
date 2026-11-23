import { useState } from 'react';
import { KeyboardAvoidingView, Platform, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button, ErrorNote, Field, Txt } from '@/components/ui';
import { useAuth } from '@/context/auth';
import { verifiedTotp, verifyCode } from '@/lib/mfa';
import { supabase } from '@/lib/supabase';
import { C, S } from '@/lib/theme';

/** Second sign-in step for users with 2FA on. The auth gate moves on by itself once the code is accepted. */
export default function TwoFactorVerify() {
  const { session } = useAuth();
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  async function submit(value = code) {
    const factor = verifiedTotp(session);
    if (!factor) return setError('No authenticator app is set up for this account. Sign out and sign in again.');
    setBusy(true);
    setError(undefined);
    try {
      await verifyCode(factor.id, value);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
      setCode('');
    } finally {
      setBusy(false);
    }
  }

  function onChange(text: string) {
    const digits = text.replace(/\D/g, '').slice(0, 6);
    setCode(digits);
    if (digits.length === 6 && !busy) submit(digits);
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: C.bg }}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={{ flex: 1, justifyContent: 'center', padding: S.xl, gap: S.lg }}>
        <View style={{ gap: S.sm }}>
          <Txt kind="title">Enter your code</Txt>
          <Txt color={C.inkSoft}>Open your authenticator app and enter the 6-digit code for Salon Books.</Txt>
        </View>
        <Field
          label="Code"
          value={code}
          onChangeText={onChange}
          keyboardType="number-pad"
          textContentType="oneTimeCode"
          autoComplete="one-time-code"
          autoFocus
          maxLength={6}
          placeholder="123456"
          style={{ fontSize: 28, fontWeight: '700', letterSpacing: 8, textAlign: 'center' }}
        />
        <ErrorNote message={error} />
        <Button title="Verify" onPress={() => submit()} loading={busy} disabled={code.length !== 6} />
        <Button kind="quiet" title="Sign out" onPress={() => supabase.auth.signOut()} />
        <Txt kind="small" color={C.inkFaint} style={{ textAlign: 'center' }}>
          Lost your phone? Ask the app admin to remove 2FA from your account.
        </Txt>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
