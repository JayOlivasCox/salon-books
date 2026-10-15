import DateTimePicker from '@react-native-community/datetimepicker';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { View } from 'react-native';

import { Button, Chip, ChipRow, ErrorNote, Field, Section, Segmented, Sheet, Txt } from '@/components/ui';
import { useCurrentBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { isoDate, longDate, parseDate } from '@/lib/format';
import { C, S } from '@/lib/theme';
import { uuid } from '@/lib/uuid';

const DURATIONS = [30, 45, 60, 90, 120, 180];

export default function NewAppointment() {
  const { date } = useLocalSearchParams<{ date?: string }>();
  const day = date ?? isoDate(new Date());
  const { business, me, clients, services, refreshLists } = useCurrentBusiness();

  const [who, setWho] = useState<'client' | 'blank'>('client');
  const [query, setQuery] = useState('');
  const [clientId, setClientId] = useState<string>();
  const [serviceTypeId, setServiceTypeId] = useState<string>();
  const [start, setStart] = useState(() => {
    const d = parseDate(day);
    d.setHours(10, 0, 0, 0);
    return d;
  });
  const [minutes, setMinutes] = useState(60);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  const q = query.trim().toLowerCase();
  const matches = q ? clients.filter((c) => c.name.toLowerCase().includes(q)).slice(0, 8) : clients.slice(0, 8);
  const exact = clients.find((c) => c.name.toLowerCase() === q);
  const picked = clients.find((c) => c.id === clientId);

  function pickService(id: string) {
    const next = id === serviceTypeId ? undefined : id;
    setServiceTypeId(next);
    const d = services.find((s) => s.id === next)?.defaultDurationMinutes;
    if (d) setMinutes(d);
  }

  async function save() {
    setBusy(true);
    setError(undefined);
    try {
      let cid = who === 'client' ? clientId : undefined;
      // Typed a new name: create the client first.
      if (who === 'client' && !cid && q) {
        cid = exact?.id ?? (await api.saveClient(business.id, { id: uuid(), name: query.trim() })).id;
        await refreshLists();
      }
      const startsAt = new Date(start);
      const endsAt = new Date(start.getTime() + minutes * 60_000);
      await api.saveAppointment(business.id, {
        id: uuid(), stylistId: me, clientId: cid, serviceTypeId,
        startsAt: startsAt.toISOString(), endsAt: endsAt.toISOString(),
      });
      router.back();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Sheet title={`New appointment, ${longDate(day)}`} onClose={() => router.back()}>
      <Segmented
        value={who}
        onChange={setWho}
        options={[{ value: 'client', label: 'Client' }, { value: 'blank', label: 'Just block the time' }]}
      />

      {who === 'client' && (
        <Section title="Client">
          {picked ? (
            <ChipRow><Chip label={`${picked.name}  ✕`} selected onPress={() => setClientId(undefined)} /></ChipRow>
          ) : (
            <>
              <Field label="Name" placeholder="Search or type a new client" value={query} onChangeText={setQuery} />
              <ChipRow wrap>
                {matches.map((c) => <Chip key={c.id} label={c.name} onPress={() => { setClientId(c.id); setQuery(''); }} />)}
              </ChipRow>
              {q && !exact && <Txt kind="small" color={C.inkSoft}>“{query.trim()}” will be added as a new client.</Txt>}
            </>
          )}
        </Section>
      )}

      <Section title="Service">
        {services.length === 0
          ? <Txt kind="small" color={C.inkSoft}>No services yet. You can add them in Settings.</Txt>
          : (
            <ChipRow wrap>
              {services.map((s) => <Chip key={s.id} label={s.name} selected={s.id === serviceTypeId} onPress={() => pickService(s.id)} />)}
            </ChipRow>
          )}
      </Section>

      <Section title="Time">
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: S.md }}>
          <Txt color={C.inkSoft}>Starts</Txt>
          <DateTimePicker value={start} mode="time" display="compact" minuteInterval={5}
            onValueChange={(_e, d) => setStart(d)} />
        </View>
        <ChipRow wrap>
          {DURATIONS.map((m) => (
            <Chip key={m} label={m < 60 ? `${m} min` : `${m / 60} hr`} selected={m === minutes} onPress={() => setMinutes(m)} />
          ))}
        </ChipRow>
      </Section>

      <ErrorNote message={error} />
      <Button title="Add appointment" onPress={save} loading={busy} />
    </Sheet>
  );
}
