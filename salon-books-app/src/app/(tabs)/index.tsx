import { router } from 'expo-router';
import { SymbolView } from 'expo-symbols';
import { useEffect, useState } from 'react';
import {
  Alert, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ErrorNote, Loading, Money, Txt } from '@/components/ui';
import { useCurrentBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { addDays, dayTitle, isoDate, money, parseMoney, sameDay, time } from '@/lib/format';
import { C, R, S, T } from '@/lib/theme';
import type { AppointmentView, IncomeEntry, PaymentMethod, PriceSuggestion } from '@/lib/types';
import { useLoad } from '@/lib/use-load';
import { uuid } from '@/lib/uuid';

const METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'CARD', label: 'Card' }, { value: 'CASH', label: 'Cash' },
  { value: 'VENMO', label: 'Venmo' }, { value: 'ZELLE', label: 'Zelle' },
];

type Selection = { kind: 'walkin' } | { kind: 'appt'; appt: AppointmentView };

export default function Today() {
  const { business, me, services } = useCurrentBusiness();
  const [date, setDate] = useState(() => new Date());
  const day = isoDate(date);
  const { data, error, loading, reload } = useLoad(() => api.daily(business.id, day), [business.id, day]);

  const [sel, setSel] = useState<Selection>({ kind: 'walkin' });
  const [serviceTypeId, setServiceTypeId] = useState<string>();
  const [amount, setAmount] = useState('');
  const [tip, setTip] = useState('');
  const [method, setMethod] = useState<PaymentMethod>('CARD');
  const [sugg, setSugg] = useState<{ key: string; value: PriceSuggestion }>();
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string>();

  const appt = sel.kind === 'appt' ? sel.appt : undefined;
  const existing: IncomeEntry | undefined = appt?.incomeEntryId
    ? data?.entries.find((e) => e.id === appt.incomeEntryId) : undefined;
  const clientId = appt?.clientId;

  /** Select an appointment (or walk-in) and load its logged amounts into the panel. */
  function choose(next: Selection) {
    const a = next.kind === 'appt' ? next.appt : undefined;
    const logged = a?.incomeEntryId ? data?.entries.find((e) => e.id === a.incomeEntryId) : undefined;
    setSel(next);
    setServiceTypeId(a?.serviceTypeId ?? logged?.serviceTypeId);
    setAmount(logged ? String(logged.serviceAmount) : '');
    setTip(logged ? String(logged.tipAmount) : '');
    setMethod(logged?.paymentMethod ?? 'CARD');
    setSaveError(undefined);
  }

  function goTo(d: Date) {
    setDate(d);
    choose({ kind: 'walkin' });
  }

  // Learned price for this client/service. Offered as a chip, never auto-filled.
  const suggKey = `${clientId ?? ''}|${serviceTypeId ?? ''}`;
  const suggestion = sugg?.key === suggKey ? sugg.value : undefined;
  useEffect(() => {
    if (!clientId && !serviceTypeId) return;
    let live = true;
    api.suggestion(business.id, clientId, serviceTypeId)
      .then((value) => { if (live) setSugg({ key: `${clientId ?? ''}|${serviceTypeId ?? ''}`, value }); })
      .catch(() => {});
    return () => { live = false; };
  }, [business.id, clientId, serviceTypeId]);

  async function save() {
    const service = parseMoney(amount);
    if (service == null) return setSaveError('Enter the service amount.');
    setSaving(true);
    setSaveError(undefined);
    try {
      await api.saveIncome(business.id, {
        id: existing?.id ?? uuid(),
        stylistId: me,
        appointmentId: appt?.id,
        clientId,
        serviceTypeId,
        serviceDate: day,
        serviceAmount: service,
        tipAmount: parseMoney(tip) ?? 0,
        paymentMethod: method,
      });
      choose({ kind: 'walkin' });
      await reload();
    } catch (e) {
      setSaveError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  }

  function removeAppointment(a: AppointmentView) {
    Alert.alert('Remove appointment?', `${time(a.startsAt)} ${a.clientName ?? 'Booked slot'}`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Remove', style: 'destructive',
        onPress: async () => {
          await api.deleteAppointment(business.id, a.id).catch(() => {});
          if (appt?.id === a.id) choose({ kind: 'walkin' });
          reload();
        },
      },
    ]);
  }

  const walkIns = data?.entries.filter((e) => !e.appointmentId) ?? [];
  const serviceName = (id?: string) => services.find((s) => s.id === id)?.name;
  const suggestionText = (() => {
    if (!suggestion || suggestion.suggestedAmount == null) return null;
    const svc = serviceName(suggestion.serviceTypeId) ?? 'this service';
    if (suggestion.source === 'CLIENT_LAST') {
      const tipPart = suggestion.suggestedTip ? ` + ${money(suggestion.suggestedTip)} tip` : '';
      return `Last ${svc}: ${money(suggestion.suggestedAmount)}${tipPart}`;
    }
    return `Your usual ${svc}: ${money(suggestion.suggestedAmount)}`;
  })();

  return (
    <SafeAreaView edges={['top']} style={{ flex: 1, backgroundColor: C.bg }}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1 }}>
        {/* Day header */}
        <View style={st.header}>
          <View style={st.dayRow}>
            <Pressable onPress={() => goTo(addDays(date, -1))} hitSlop={12} accessibilityLabel="Previous day">
              <SymbolView name="chevron.left" size={20} tintColor={C.toner} />
            </Pressable>
            <Pressable style={{ flex: 1 }} onPress={() => goTo(new Date())}>
              <Txt kind="title">{sameDay(date, new Date()) ? 'Today' : dayTitle(date)}</Txt>
              {sameDay(date, new Date()) && <Txt kind="small" color={C.inkSoft}>{dayTitle(date)}</Txt>}
            </Pressable>
            <Pressable onPress={() => goTo(addDays(date, 1))} hitSlop={12} accessibilityLabel="Next day">
              <SymbolView name="chevron.right" size={20} tintColor={C.toner} />
            </Pressable>
          </View>
          <View style={st.totals}>
            <View style={{ flex: 1 }}>
              <Txt kind="small" color={C.inkSoft}>Services</Txt>
              <Money value={data?.totals.services} size={18} />
            </View>
            <View style={{ flex: 1 }}>
              <Txt kind="small" color={C.inkSoft}>Tips</Txt>
              <Money value={data?.totals.tips} size={18} />
            </View>
            <View style={{ flex: 1.3, alignItems: 'flex-end' }}>
              <Txt kind="small" color={C.inkSoft}>Made today</Txt>
              <Money value={data?.totals.gross} size={26} color={C.income} />
            </View>
          </View>
        </View>

        {/* Schedule */}
        <ScrollView style={{ flex: 1 }} contentContainerStyle={st.list}>
          <ErrorNote message={error} />
          {loading && !data && <Loading />}
          {data && data.appointments.length === 0 && walkIns.length === 0 && (
            <Txt color={C.inkSoft}>No appointments. Add one, or log a walk-in below.</Txt>
          )}
          {data && data.unnamedCount > 0 && (
            <Txt kind="small" color={C.inkSoft}>
              {data.bookedCount} booked, {data.unnamedCount} without a client name
            </Txt>
          )}
          {data?.appointments.map((a) => {
            const selected = appt?.id === a.id;
            const logged = data.entries.find((e) => e.id === a.incomeEntryId);
            return (
              <Pressable key={a.id} onPress={() => choose(selected ? { kind: 'walkin' } : { kind: 'appt', appt: a })}
                onLongPress={() => removeAppointment(a)}
                style={[st.slot, selected && st.slotOn]}>
                <View style={st.slotTime}>
                  <Text style={[T.label, { color: selected ? C.toner : C.ink }]}>{time(a.startsAt)}</Text>
                  <Txt kind="small" color={C.inkFaint}>{time(a.endsAt)}</Txt>
                </View>
                <View style={{ flex: 1 }}>
                  <Txt kind="label" color={a.clientName ? C.ink : C.inkSoft}>{a.clientName ?? 'Booked'}</Txt>
                  {a.serviceName && <Txt kind="small" color={C.inkSoft}>{a.serviceName}</Txt>}
                </View>
                {logged ? (
                  <View style={{ alignItems: 'flex-end' }}>
                    <Money value={logged.serviceAmount + logged.tipAmount} color={C.income} />
                    <Txt kind="small" color={C.inkSoft}>logged</Txt>
                  </View>
                ) : (
                  <SymbolView name="plus.circle" size={22} tintColor={selected ? C.toner : C.inkFaint} />
                )}
              </Pressable>
            );
          })}
          {walkIns.map((e) => (
            <View key={e.id} style={st.slot}>
              <View style={st.slotTime}><Txt kind="label">Walk-in</Txt></View>
              <Txt kind="small" color={C.inkSoft} style={{ flex: 1 }}>{serviceName(e.serviceTypeId) ?? ''}</Txt>
              <Money value={e.serviceAmount + e.tipAmount} color={C.income} />
            </View>
          ))}
          <Pressable onPress={() => router.push({ pathname: '/appointment-new', params: { date: day } })} style={st.addSlot}>
            <SymbolView name="plus" size={16} tintColor={C.toner} />
            <Txt kind="label" color={C.toner}>Add appointment</Txt>
          </Pressable>
        </ScrollView>

        {/* The chair: quick entry between clients */}
        <View style={st.chair}>
          <View style={st.chairHead}>
            <Text style={[T.heading, { color: C.chairText }]} numberOfLines={1}>
              {appt ? `${time(appt.startsAt)}  ${appt.clientName ?? 'Booked client'}` : 'Walk-in'}
            </Text>
            {appt && (
              <Pressable onPress={() => choose({ kind: 'walkin' })} hitSlop={10}>
                <Txt kind="label" color={C.chairSoft}>Clear</Txt>
              </Pressable>
            )}
          </View>

          {services.length > 0 ? (
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: S.sm }}>
              {services.map((s) => {
                const on = s.id === serviceTypeId;
                return (
                  <Pressable key={s.id} onPress={() => setServiceTypeId(on ? undefined : s.id)}
                    style={[st.chairChip, on && { backgroundColor: C.chairText }]}>
                    <Text style={[T.label, { color: on ? C.chair : C.chairText }]}>{s.name}</Text>
                  </Pressable>
                );
              })}
            </ScrollView>
          ) : (
            <Pressable onPress={() => router.push('/services')}>
              <Txt kind="small" color={C.chairSoft}>Add your services in Settings to get price suggestions.</Txt>
            </Pressable>
          )}

          {suggestionText && (
            <Pressable
              onPress={() => {
                setAmount(String(suggestion!.suggestedAmount));
                if (suggestion!.suggestedTip != null) setTip(String(suggestion!.suggestedTip));
              }}
              style={st.suggest}>
              <SymbolView name="sparkles" size={14} tintColor={C.chair} />
              <Text style={[T.label, { color: C.chair, flex: 1 }]} numberOfLines={1}>{suggestionText}</Text>
              <Text style={[T.label, { color: C.toner }]}>Use</Text>
            </Pressable>
          )}

          <View style={st.amounts}>
            <AmountInput label="Service" value={amount} onChange={setAmount} />
            <AmountInput label="Tip" value={tip} onChange={setTip} />
          </View>

          <View style={{ flexDirection: 'row', gap: S.sm }}>
            {METHODS.map((m) => (
              <Pressable key={m.value} onPress={() => setMethod(m.value)}
                style={[st.method, method === m.value && { borderColor: C.chairText }]}>
                <Text style={[T.small, { color: method === m.value ? C.chairText : C.chairSoft }]}>{m.label}</Text>
              </Pressable>
            ))}
          </View>

          {saveError && <Txt kind="small" color="#FFB3C1">{saveError}</Txt>}
          <Pressable onPress={save} disabled={saving} style={({ pressed }) => [st.save, (pressed || saving) && { opacity: 0.7 }]}>
            <Text style={[T.label, { color: '#fff', fontSize: 17 }]}>
              {saving ? 'Saving...' : existing ? 'Update entry' : `Save ${money((parseMoney(amount) ?? 0) + (parseMoney(tip) ?? 0))}`}
            </Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function AmountInput({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (
    <View style={st.amountBox}>
      <Txt kind="small" color={C.chairSoft}>{label}</Txt>
      <View style={{ flexDirection: 'row', alignItems: 'baseline' }}>
        <Text style={[T.money, st.amountText, { color: C.chairSoft }]}>$</Text>
        <TextInput
          value={value}
          onChangeText={onChange}
          keyboardType="decimal-pad"
          placeholder="0"
          placeholderTextColor={C.chairSoft}
          style={[T.money, st.amountText, { flex: 1, color: C.chairText }]}
          accessibilityLabel={`${label} amount`}
        />
      </View>
    </View>
  );
}

const st = StyleSheet.create({
  header: { paddingHorizontal: S.lg, paddingTop: S.sm, paddingBottom: S.md, gap: S.md },
  dayRow: { flexDirection: 'row', alignItems: 'center', gap: S.md },
  totals: { flexDirection: 'row', alignItems: 'flex-end', gap: S.md },
  list: { paddingHorizontal: S.lg, paddingBottom: S.lg, gap: S.sm },
  slot: {
    flexDirection: 'row', alignItems: 'center', gap: S.md, backgroundColor: C.surface,
    borderRadius: R.sm, padding: S.md, borderWidth: 1.5, borderColor: 'transparent',
  },
  slotOn: { borderColor: C.toner, backgroundColor: C.tonerSoft },
  slotTime: { width: 72 },
  addSlot: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: S.sm, padding: S.md,
    borderRadius: R.sm, borderWidth: 1, borderStyle: 'dashed', borderColor: C.toner,
  },
  chair: {
    backgroundColor: C.chair, borderTopLeftRadius: R.lg, borderTopRightRadius: R.lg,
    padding: S.lg, gap: S.md,
  },
  chairHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  chairChip: { paddingHorizontal: 14, paddingVertical: 7, borderRadius: R.pill, backgroundColor: C.chairRaised },
  suggest: {
    flexDirection: 'row', alignItems: 'center', gap: S.sm, backgroundColor: C.tonerSoft,
    paddingHorizontal: S.md, paddingVertical: 9, borderRadius: R.sm,
  },
  amounts: { flexDirection: 'row', gap: S.md },
  amountBox: { flex: 1, backgroundColor: C.chairRaised, borderRadius: R.sm, paddingHorizontal: S.md, paddingVertical: S.sm },
  amountText: { fontSize: 34, paddingVertical: 2 },
  method: { flex: 1, alignItems: 'center', paddingVertical: 7, borderRadius: R.sm, borderWidth: 1, borderColor: C.chairRaised },
  save: { backgroundColor: C.toner, borderRadius: R.md, minHeight: 52, alignItems: 'center', justifyContent: 'center' },
});
