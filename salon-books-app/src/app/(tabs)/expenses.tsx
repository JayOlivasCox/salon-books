import { router } from 'expo-router';
import { SymbolView } from 'expo-symbols';
import { useState } from 'react';
import { FlatList, Pressable, StyleSheet, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Chip, ChipRow, ErrorNote, Loading, Money, OfflineNote, Stepper, Txt } from '@/components/ui';
import { useCurrentBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { isoDate, MONTHS, shortDate } from '@/lib/format';
import { C, R, S, T } from '@/lib/theme';
import type { ExpenseSort } from '@/lib/types';
import { useLoad } from '@/lib/use-load';

const SORTS: { value: ExpenseSort; label: string }[] = [
  { value: 'DATE', label: 'Date' }, { value: 'CATEGORY', label: 'Category' },
  { value: 'VENDOR', label: 'Vendor' }, { value: 'AMOUNT', label: 'Amount' },
];

export default function Expenses() {
  const { business, categories } = useCurrentBusiness();
  const [month, setMonth] = useState(() => {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1);
  });
  const [sort, setSort] = useState<ExpenseSort>(business.defaultExpenseSort);
  const [categoryId, setCategoryId] = useState<string>();
  const [vendor, setVendor] = useState('');

  const from = isoDate(month);
  const to = isoDate(new Date(month.getFullYear(), month.getMonth() + 1, 0));
  const dir = sort === 'VENDOR' || sort === 'CATEGORY' ? 'asc' : 'desc';

  const { data, error, offline, savedAt } = useLoad(
    () => api.expenses(business.id, { from, to, categoryId, vendor: vendor.trim() || undefined, sort, dir }),
    [business.id, from, to, categoryId, vendor, sort],
    { cache: 'expenses' },
  );

  const cat = (id?: string) => categories.find((c) => c.id === id);
  const total = data?.items.reduce((s, e) => s + e.total, 0) ?? 0;
  const shift = (n: number) => setMonth(new Date(month.getFullYear(), month.getMonth() + n, 1));

  return (
    <SafeAreaView edges={['top']} style={{ flex: 1, backgroundColor: C.bg }}>
      <FlatList
        data={data?.items ?? []}
        keyExtractor={(e) => e.id}
        contentContainerStyle={{ padding: S.lg, gap: S.sm, paddingBottom: 120 }}
        keyboardShouldPersistTaps="handled"
        ListHeaderComponent={
          <View style={{ gap: S.md, marginBottom: S.sm }}>
            <Stepper label={`${MONTHS[month.getMonth()]} ${month.getFullYear()}`}
              onPrev={() => shift(-1)} onNext={() => shift(1)}
              onReset={() => setMonth(new Date(new Date().getFullYear(), new Date().getMonth(), 1))} />
            <View style={{ alignItems: 'center', gap: 2 }}>
              {data ? <Money value={total} size={34} color={C.expense} /> : <Loading />}
              <Txt kind="small" color={C.inkSoft}>
                {data ? `${data.totalItems} ${data.totalItems === 1 ? 'expense' : 'expenses'}` : ' '}
              </Txt>
            </View>
            <View style={st.search}>
              <SymbolView name="magnifyingglass" size={16} tintColor={C.inkFaint} />
              <TextInput value={vendor} onChangeText={setVendor} placeholder="Filter by vendor"
                placeholderTextColor={C.inkFaint} style={[T.body, { flex: 1, color: C.ink }]} autoCorrect={false} />
            </View>
            <ChipRow>
              <Chip label="All categories" selected={!categoryId} onPress={() => setCategoryId(undefined)} />
              {categories.map((c) => (
                <Chip key={c.id} label={c.name} dot={c.color} selected={c.id === categoryId}
                  onPress={() => setCategoryId(c.id === categoryId ? undefined : c.id)} />
              ))}
            </ChipRow>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: S.sm }}>
              <Txt kind="small" color={C.inkSoft}>Sort</Txt>
              <ChipRow>
                {SORTS.map((s) => <Chip key={s.value} label={s.label} selected={sort === s.value} onPress={() => setSort(s.value)} />)}
              </ChipRow>
            </View>
            <ErrorNote message={error} />
            {offline && <OfflineNote savedAt={savedAt} />}
          </View>
        }
        ListEmptyComponent={data ? (
          <Txt color={C.inkSoft} style={{ textAlign: 'center', marginTop: S.lg }}>
            No expenses here. Tap Add expense to log a receipt.
          </Txt>
        ) : null}
        renderItem={({ item }) => {
          const c = cat(item.categoryId);
          return (
            <Pressable onPress={() => router.push({ pathname: '/expense-new', params: { id: item.id } })} style={st.row}>
              <View style={[st.swatch, { backgroundColor: c?.color ?? C.line }]} />
              <View style={{ flex: 1, gap: 2 }}>
                <Txt kind="label" numberOfLines={1}>{item.vendor ?? 'No vendor'}</Txt>
                <Txt kind="small" color={C.inkSoft} numberOfLines={1}>
                  {shortDate(item.expenseDate)}, {c?.name ?? 'Uncategorized'}{item.itemized ? `, ${item.items.length} items` : ''}
                </Txt>
              </View>
              <Money value={item.total} />
            </Pressable>
          );
        }}
      />
      <Pressable onPress={() => router.push('/expense-new')} style={st.fab} accessibilityRole="button">
        <SymbolView name="plus" size={18} tintColor="#fff" />
        <Txt kind="label" color="#fff">Add expense</Txt>
      </Pressable>
    </SafeAreaView>
  );
}

const st = StyleSheet.create({
  search: {
    flexDirection: 'row', alignItems: 'center', gap: S.sm, backgroundColor: C.surface,
    borderRadius: R.sm, paddingHorizontal: S.md, paddingVertical: 10,
  },
  row: { flexDirection: 'row', alignItems: 'center', gap: S.md, backgroundColor: C.surface, borderRadius: R.sm, padding: S.md },
  swatch: { width: 6, alignSelf: 'stretch', borderRadius: 3 },
  fab: {
    position: 'absolute', right: S.lg, bottom: S.lg, flexDirection: 'row', alignItems: 'center', gap: S.sm,
    backgroundColor: C.toner, paddingHorizontal: S.lg, paddingVertical: 14, borderRadius: R.pill,
  },
});
