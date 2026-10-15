import DateTimePicker from '@react-native-community/datetimepicker';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { View } from 'react-native';

import { Button, Chip, ChipRow, ErrorNote, Field, Loading, Section, Segmented, Sheet, Txt } from '@/components/ui';
import { useCurrentBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { isoDate, longDate, parseDate, parseMoney } from '@/lib/format';
import { C, S } from '@/lib/theme';
import type { RentFrequency } from '@/lib/types';
import { uuid } from '@/lib/uuid';

const LENGTHS = [3, 6, 12, 24];

export default function LeaseForm() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const { business } = useCurrentBusiness();
  const [loaded, setLoaded] = useState(!id);
  const [amount, setAmount] = useState('');
  const [frequency, setFrequency] = useState<RentFrequency>('MONTHLY');
  const [start, setStart] = useState(() => {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1);
  });
  const [months, setMonths] = useState<number | 'custom'>(12);
  const [end, setEnd] = useState(() => new Date(new Date().getFullYear() + 1, new Date().getMonth(), 0));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  useEffect(() => {
    if (!id) return;
    api.leases(business.id).then((list) => {
      const l = list.find((x) => x.id === id);
      if (l) {
        setAmount(String(l.amount));
        setFrequency(l.frequency);
        setStart(parseDate(l.startDate));
        setEnd(parseDate(l.endDate));
        setMonths('custom');
      }
      setLoaded(true);
    });
  }, [business.id, id]);

  const computedEnd = months === 'custom' ? end : new Date(start.getFullYear(), start.getMonth() + months, start.getDate() - 1);

  async function save() {
    const a = parseMoney(amount);
    if (!a || a <= 0) return setError('Enter how much rent is.');
    setBusy(true);
    setError(undefined);
    try {
      await api.saveLease(business.id, {
        id: id ?? uuid(), amount: a, frequency, startDate: isoDate(start),
        ...(months === 'custom' ? { endDate: isoDate(end) } : { durationMonths: months }),
      });
      router.back();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Sheet title="Booth rent" onClose={() => router.back()}>
      {!loaded ? <Loading /> : (
        <>
          <Field label="Rent" placeholder="0.00" value={amount} onChangeText={setAmount} keyboardType="decimal-pad"
            style={{ fontSize: 24, fontWeight: '700' }} />
          <Segmented value={frequency} onChange={setFrequency}
            options={[{ value: 'MONTHLY', label: 'Per month' }, { value: 'WEEKLY', label: 'Per week' }]} />

          <View style={{ flexDirection: 'row', alignItems: 'center', gap: S.md }}>
            <Txt color={C.inkSoft}>Starts</Txt>
            <DateTimePicker value={start} mode="date" display="compact" onValueChange={(_e, d) => setStart(d)} />
          </View>

          <Section title="How long is your contract?">
            <ChipRow wrap>
              {LENGTHS.map((m) => <Chip key={m} label={`${m} months`} selected={months === m} onPress={() => setMonths(m)} />)}
              <Chip label="Pick end date" selected={months === 'custom'} onPress={() => setMonths('custom')} />
            </ChipRow>
            {months === 'custom' ? (
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: S.md }}>
                <Txt color={C.inkSoft}>Ends</Txt>
                <DateTimePicker value={end} mode="date" display="compact" minimumDate={start} onValueChange={(_e, d) => setEnd(d)} />
              </View>
            ) : (
              <Txt kind="small" color={C.inkSoft}>Ends {longDate(isoDate(computedEnd))}</Txt>
            )}
          </Section>

          <Txt kind="small" color={C.inkSoft}>
            Rent is added to your reports and tax summary automatically, so you don’t need to log it as an expense.
          </Txt>
          <ErrorNote message={error} />
          <Button title="Save booth rent" onPress={save} loading={busy} />
        </>
      )}
    </Sheet>
  );
}
