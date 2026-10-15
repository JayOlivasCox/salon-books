import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, Pressable, View } from 'react-native';
import { SymbolView } from 'expo-symbols';

import { Button, Chip, ChipRow, ErrorNote, Field, Panel, Section, Sheet, Txt } from '@/components/ui';
import { useCurrentBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { C, S } from '@/lib/theme';
import { uuid } from '@/lib/uuid';

const DURATIONS = [30, 45, 60, 90, 120, 180];

export default function Services() {
  const { business, services, refreshLists } = useCurrentBusiness();
  const [name, setName] = useState('');
  const [minutes, setMinutes] = useState<number>();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  async function add() {
    setBusy(true);
    try {
      await api.saveService(business.id, { id: uuid(), name: name.trim(), defaultDurationMinutes: minutes });
      await refreshLists();
      setName('');
      setMinutes(undefined);
      setError(undefined);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  function remove(id: string, label: string) {
    Alert.alert(`Remove ${label}?`, 'Past entries keep their records.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Remove', style: 'destructive', onPress: async () => { await api.deleteService(business.id, id); refreshLists(); } },
    ]);
  }

  return (
    <Sheet title="Services" onClose={() => router.back()}>
      <Txt color={C.inkSoft}>
        Services don’t have fixed prices. The app learns what you charge each client and offers it next time.
      </Txt>
      <Panel>
        <Field label="Service name" placeholder="e.g. Balayage" value={name} onChangeText={setName} />
        <Section title="Usual length (optional)">
          <ChipRow wrap>
            {DURATIONS.map((m) => (
              <Chip key={m} label={m < 60 ? `${m} min` : `${m / 60} hr`} selected={m === minutes}
                onPress={() => setMinutes(m === minutes ? undefined : m)} />
            ))}
          </ChipRow>
        </Section>
        <ErrorNote message={error} />
        <Button title="Add service" onPress={add} loading={busy} disabled={!name.trim()} />
      </Panel>
      <View style={{ gap: S.sm }}>
        {services.map((s) => (
          <View key={s.id} style={{ flexDirection: 'row', alignItems: 'center', backgroundColor: C.surface, padding: S.md, borderRadius: 8 }}>
            <View style={{ flex: 1 }}>
              <Txt kind="label">{s.name}</Txt>
              {s.defaultDurationMinutes && <Txt kind="small" color={C.inkSoft}>{s.defaultDurationMinutes} min</Txt>}
            </View>
            <Pressable onPress={() => remove(s.id, s.name)} hitSlop={10} accessibilityLabel={`Remove ${s.name}`}>
              <SymbolView name="trash" size={18} tintColor={C.expense} />
            </Pressable>
          </View>
        ))}
      </View>
    </Sheet>
  );
}
