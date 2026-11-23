import { router } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Linking, Share, Text, View } from 'react-native';
import QRCode from 'react-native-qrcode-svg';

import { Button, ErrorNote, Field, Loading, Panel, Section, Sheet, Txt } from '@/components/ui';
import { codeError, verifyCode } from '@/lib/mfa';
import { supabase } from '@/lib/supabase';
import { C, S, T } from '@/lib/theme';

type Pending = { factorId: string; uri: string; secret: string };

/** Turn on 2FA: add Salon Books to an authenticator app, then confirm with a code. */
export default function TwoFactorSetup() {
  const [pending, setPending] = useState<Pending>();
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();
  const [showKey, setShowKey] = useState(false);
  const done = useRef(false);
  const pendingId = useRef<string | undefined>(undefined);

  useEffect(() => {
    let live = true;
    (async () => {
      try {
        // Clear any half-finished setup from before, so the new one doesn't clash with it.
        const { data: list } = await supabase.auth.mfa.listFactors();
        for (const f of list?.all ?? []) {
          if (f.factor_type === 'totp' && f.status === 'unverified') {
            await supabase.auth.mfa.unenroll({ factorId: f.id });
          }
        }
        const { data, error: err } = await supabase.auth.mfa.enroll({
          factorType: 'totp',
          issuer: 'Salon Books',
          friendlyName: 'Salon Books',
        });
        if (err) throw err;
        pendingId.current = data.id;
        if (live) setPending({ factorId: data.id, uri: data.totp.uri, secret: data.totp.secret });
      } catch (e) {
        if (live) setError(codeError(e instanceof Error ? e.message : String(e)));
      }
    })();
    return () => {
      live = false;
      // Closed before confirming: remove the unfinished setup.
      if (!done.current && pendingId.current) {
        supabase.auth.mfa.unenroll({ factorId: pendingId.current }).catch(() => {});
      }
    };
  }, []);

  async function confirm(value = code) {
    if (!pending) return;
    setBusy(true);
    setError(undefined);
    try {
      await verifyCode(pending.factorId, value);
      done.current = true;
      router.back();
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
    if (digits.length === 6 && !busy) confirm(digits);
  }

  async function openAuthenticator() {
    if (!pending) return;
    try {
      await Linking.openURL(pending.uri);
    } catch {
      setShowKey(true);
      setError('No authenticator app found. Install one (like Google Authenticator), or enter the setup key by hand.');
    }
  }

  return (
    <Sheet title="Two-factor authentication" onClose={() => router.back()}>
      <Txt color={C.inkSoft}>
        After this, signing in needs your password and a 6-digit code from an authenticator app,
        so someone with only your password can&apos;t see your books.
      </Txt>

      {!pending ? (error ? <ErrorNote message={error} /> : <Loading />) : (
        <>
          <Section title="1. Add Salon Books to your authenticator">
            <Panel style={{ alignItems: 'center' }}>
              <Button icon="key" title="Open authenticator app" onPress={openAuthenticator} style={{ alignSelf: 'stretch' }} />
              <Txt kind="small" color={C.inkSoft} style={{ textAlign: 'center' }}>
                Or scan this from another phone or tablet:
              </Txt>
              <QRCode value={pending.uri} size={170} color={C.ink} backgroundColor={C.surface} />
              <Button kind="quiet" title={showKey ? 'Hide setup key' : 'Enter a setup key instead'}
                onPress={() => setShowKey(!showKey)} />
              {showKey && (
                <View style={{ gap: S.sm, alignSelf: 'stretch' }}>
                  <Text selectable style={[T.label, { color: C.ink, textAlign: 'center', letterSpacing: 1 }]}>
                    {pending.secret}
                  </Text>
                  <Button kind="secondary" icon="square.and.arrow.up" title="Copy or share key"
                    onPress={() => Share.share({ message: pending.secret })} />
                </View>
              )}
            </Panel>
          </Section>

          <Section title="2. Enter the 6-digit code it shows">
            <Field
              label="Code"
              value={code}
              onChangeText={onChange}
              keyboardType="number-pad"
              textContentType="oneTimeCode"
              autoComplete="one-time-code"
              maxLength={6}
              placeholder="123456"
              style={{ fontSize: 28, fontWeight: '700', letterSpacing: 8, textAlign: 'center' }}
            />
          </Section>

          <ErrorNote message={error} />
          <Button title="Turn on 2FA" onPress={() => confirm()} loading={busy} disabled={code.length !== 6} />
        </>
      )}
    </Sheet>
  );
}
