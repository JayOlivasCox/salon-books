import { useState } from 'react';
import { KeyboardAvoidingView, Platform, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button, ErrorNote, Field, Txt } from '@/components/ui';
import { supabase } from '@/lib/supabase';
import { C, S } from '@/lib/theme';

export default function SignIn() {
  const [mode, setMode] = useState<'in' | 'up'>('in');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();
  const [notice, setNotice] = useState<string>();

  async function submit() {
    setBusy(true);
    setError(undefined);
    setNotice(undefined);
    const creds = { email: email.trim(), password };
    const { data, error: err } = mode === 'in'
      ? await supabase.auth.signInWithPassword(creds)
      : await supabase.auth.signUp(creds);
    setBusy(false);
    if (err) return setError(err.message);
    if (mode === 'up' && !data.session) setNotice('Check your email to confirm your account, then sign in.');
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: C.chair }}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1, justifyContent: 'flex-end' }}>
        <View style={{ padding: S.xl, gap: S.sm }}>
          <Txt kind="display" color={C.chairText}>Salon Books</Txt>
          <Txt color={C.chairSoft}>Track every service, tip, and receipt between clients.</Txt>
        </View>
        <View style={{ backgroundColor: C.bg, padding: S.xl, gap: S.lg, borderTopLeftRadius: 28, borderTopRightRadius: 28 }}>
          <Field label="Email" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address"
            autoComplete="email" textContentType="emailAddress" />
          <Field label="Password" value={password} onChangeText={setPassword} secureTextEntry
            textContentType={mode === 'in' ? 'password' : 'newPassword'} />
          <ErrorNote message={error} />
          {notice && <Txt kind="small" color={C.income}>{notice}</Txt>}
          <Button title={mode === 'in' ? 'Sign in' : 'Create account'} onPress={submit} loading={busy}
            disabled={!email || password.length < 6} />
          <Button kind="quiet" title={mode === 'in' ? 'New here? Create an account' : 'Have an account? Sign in'}
            onPress={() => setMode(mode === 'in' ? 'up' : 'in')} />
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
