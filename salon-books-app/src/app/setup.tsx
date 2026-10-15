import { router } from 'expo-router';
import { useState } from 'react';

import { Button, ErrorNote, Field, Panel, Screen, Txt } from '@/components/ui';
import { useBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { supabase } from '@/lib/supabase';
import { C } from '@/lib/theme';

export default function Setup() {
  const { setBusiness } = useBusiness();
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  async function create() {
    setBusy(true);
    try {
      const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'America/Los_Angeles';
      setBusiness(await api.createBusiness(name.trim(), tz));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Screen>
      <Txt kind="title">Set up your books</Txt>
      <Panel>
        <Txt kind="heading">Start your own</Txt>
        <Field label="Business name" placeholder="e.g. Jay's Chair at Studio 9" value={name} onChangeText={setName} />
        <ErrorNote message={error} />
        <Button title="Create business" onPress={create} loading={busy} disabled={!name.trim()} />
      </Panel>
      <Panel>
        <Txt kind="heading">Share a business with a partner</Txt>
        <Txt color={C.inkSoft}>Ask them to open Settings and tap Invite partner, then scan their code.</Txt>
        <Button kind="secondary" icon="qrcode.viewfinder" title="Scan invite code" onPress={() => router.push('/join')} />
      </Panel>
      <Button kind="quiet" title="Sign out" onPress={() => supabase.auth.signOut()} />
    </Screen>
  );
}
