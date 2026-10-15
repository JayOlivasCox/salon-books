import DateTimePicker from '@react-native-community/datetimepicker';
import { router, useLocalSearchParams } from 'expo-router';
import { SymbolView } from 'expo-symbols';
import { useEffect, useState } from 'react';
import { Alert, Pressable, TextInput, View } from 'react-native';

import { Button, Chip, ChipRow, ErrorNote, Field, Loading, Money, Section, Sheet, Txt, styles as ui } from '@/components/ui';
import { useCurrentBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { isoDate, parseDate, parseMoney } from '@/lib/format';
import { C, S } from '@/lib/theme';
import { uuid } from '@/lib/uuid';

type ItemDraft = { id: string; description: string; amount: string };

export default function ExpenseForm() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const editing = !!id;
  const { business, categories } = useCurrentBusiness();

  const [loaded, setLoaded] = useState(!editing);
  const [date, setDate] = useState(new Date());
  const [vendor, setVendor] = useState('');
  const [total, setTotal] = useState('');
  const [categoryId, setCategoryId] = useState<string>();
  const [notes, setNotes] = useState('');
  const [items, setItems] = useState<ItemDraft[]>([]);
  const [vendors, setVendors] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  useEffect(() => {
    api.vendors(business.id).then(setVendors).catch(() => {});
    if (!id) return;
    api.expense(business.id, id).then((e) => {
      setDate(parseDate(e.expenseDate));
      setVendor(e.vendor ?? '');
      setTotal(String(e.total));
      setCategoryId(e.categoryId);
      setNotes(e.notes ?? '');
      setItems(e.items.map((i) => ({ id: i.id, description: i.description, amount: String(i.amount) })));
      setLoaded(true);
    }).catch((e) => setError(String(e.message ?? e)));
  }, [business.id, id]);

  const itemSum = items.reduce((s, i) => s + (parseMoney(i.amount) ?? 0), 0);
  const v = vendor.trim().toLowerCase();
  const vendorMatches = v ? vendors.filter((x) => x.toLowerCase().includes(v) && x.toLowerCase() !== v).slice(0, 5) : [];

  const setItem = (i: number, patch: Partial<ItemDraft>) =>
    setItems((prev) => prev.map((it, j) => (j === i ? { ...it, ...patch } : it)));

  async function save() {
    const t = parseMoney(total) ?? (items.length ? itemSum : null);
    if (t == null) return setError('Enter the total.');
    setBusy(true);
    setError(undefined);
    try {
      await api.saveExpense(business.id, {
        id: id ?? uuid(),
        expenseDate: isoDate(date),
        vendor: vendor.trim() || undefined,
        total: t,
        categoryId,
        notes: notes.trim() || undefined,
        items: items
          .filter((i) => i.description.trim() && parseMoney(i.amount) != null)
          .map((i) => ({ id: i.id, description: i.description.trim(), amount: parseMoney(i.amount)! })),
      });
      router.back();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  }

  function remove() {
    Alert.alert('Delete this expense?', undefined, [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: async () => { await api.deleteExpense(business.id, id!); router.back(); } },
    ]);
  }

  return (
    <Sheet title={editing ? 'Edit expense' : 'Add expense'} onClose={() => router.back()}>
      {!loaded ? <Loading /> : (
        <>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: S.md }}>
            <Txt color={C.inkSoft}>Date</Txt>
            <DateTimePicker value={date} mode="date" display="compact" onValueChange={(_e, d) => setDate(d)} />
          </View>

          <View style={{ gap: S.sm }}>
            <Field label="Vendor" placeholder="e.g. Sally Beauty" value={vendor} onChangeText={setVendor} />
            {vendorMatches.length > 0 && (
              <ChipRow>{vendorMatches.map((m) => <Chip key={m} label={m} onPress={() => setVendor(m)} />)}</ChipRow>
            )}
          </View>

          <Field label="Total" placeholder={items.length ? String(itemSum.toFixed(2)) : '0.00'} value={total}
            onChangeText={setTotal} keyboardType="decimal-pad" style={{ fontSize: 24, fontWeight: '700' }} />

          <Section title="Category">
            <ChipRow wrap>
              {categories.map((c) => (
                <Chip key={c.id} label={c.name} dot={c.color} selected={c.id === categoryId}
                  onPress={() => setCategoryId(c.id === categoryId ? undefined : c.id)} />
              ))}
            </ChipRow>
          </Section>

          <Section
            title="Items"
            action={<Button kind="quiet" icon="plus" title="Add item"
              onPress={() => setItems([...items, { id: uuid(), description: '', amount: '' }])} />}>
            {items.length === 0 && <Txt kind="small" color={C.inkSoft}>Optional. List what was on the receipt.</Txt>}
            {items.map((it, i) => (
              <View key={it.id} style={{ flexDirection: 'row', gap: S.sm, alignItems: 'center' }}>
                <TextInput style={[ui.input, { flex: 2 }]} placeholder="Item" placeholderTextColor={C.inkFaint}
                  value={it.description} onChangeText={(t) => setItem(i, { description: t })} />
                <TextInput style={[ui.input, { flex: 1 }]} placeholder="0.00" placeholderTextColor={C.inkFaint}
                  keyboardType="decimal-pad" value={it.amount} onChangeText={(t) => setItem(i, { amount: t })} />
                <Pressable onPress={() => setItems(items.filter((_, j) => j !== i))} hitSlop={8} accessibilityLabel="Remove item">
                  <SymbolView name="minus.circle" size={22} tintColor={C.expense} />
                </Pressable>
              </View>
            ))}
            {items.length > 0 && (
              <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
                <Txt kind="small" color={C.inkSoft}>Items add up to</Txt>
                <Money value={itemSum} size={14} color={C.inkSoft} />
              </View>
            )}
          </Section>

          <Field label="Notes" value={notes} onChangeText={setNotes} multiline style={{ minHeight: 70 }} />
          <ErrorNote message={error} />
          <Button title={editing ? 'Save changes' : 'Save expense'} onPress={save} loading={busy} />
          {editing && <Button kind="danger" title="Delete expense" onPress={remove} />}
        </>
      )}
    </Sheet>
  );
}
