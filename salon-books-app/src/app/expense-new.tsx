import DateTimePicker from '@react-native-community/datetimepicker';
import { router, useLocalSearchParams } from 'expo-router';
import { SymbolView } from 'expo-symbols';
import { useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Image, Modal, Pressable, TextInput, View } from 'react-native';

import { Button, Chip, ChipRow, ErrorNote, Field, Loading, Money, Section, Sheet, Txt, styles as ui } from '@/components/ui';
import { useCurrentBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { isoDate, parseDate, parseMoney } from '@/lib/format';
import { ocrAvailable, pickReceiptPhoto, readReceipt, uploadReceipt, type Photo } from '@/lib/receipt';
import { C, R, S } from '@/lib/theme';
import { uuid } from '@/lib/uuid';

type ItemDraft = { id: string; description: string; amount: string };

export default function ExpenseForm() {
  const { id: editId } = useLocalSearchParams<{ id?: string }>();
  const editing = !!editId;
  // Fixed up front so a receipt photo can be stored under this expense before it's saved.
  const [id] = useState(() => editId ?? uuid());
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

  // Receipt: a new photo waiting to upload, the one already saved, and the scan state.
  const [photo, setPhoto] = useState<Photo>();
  const [receiptPath, setReceiptPath] = useState<string>();
  const [savedPhotoUrl, setSavedPhotoUrl] = useState<string>();
  const [ocrText, setOcrText] = useState<string>();
  const [scanning, setScanning] = useState(false);
  const [scanNote, setScanNote] = useState<string>();
  const [viewing, setViewing] = useState(false);

  useEffect(() => {
    api.vendors(business.id).then(setVendors).catch(() => {});
    if (!editId) return;
    api.expense(business.id, editId).then((e) => {
      setDate(parseDate(e.expenseDate));
      setVendor(e.vendor ?? '');
      setTotal(String(e.total));
      setCategoryId(e.categoryId);
      setNotes(e.notes ?? '');
      setItems(e.items.map((i) => ({ id: i.id, description: i.description, amount: String(i.amount) })));
      setReceiptPath(e.receiptPath);
      setLoaded(true);
      if (e.receiptPath) {
        api.receiptUrl(business.id, editId).then((r) => setSavedPhotoUrl(r.url)).catch(() => {});
      }
    }).catch((e) => setError(String(e.message ?? e)));
  }, [business.id, editId]);

  async function scan(source: 'camera' | 'library') {
    setScanNote(undefined);
    let picked: Photo | undefined;
    try {
      picked = await pickReceiptPhoto(source);
    } catch (e) {
      setScanNote(e instanceof Error ? e.message : String(e));
      return;
    }
    if (!picked) return;
    setPhoto(picked);
    if (!ocrAvailable) {
      setScanNote('Photo attached. Reading receipts needs a new build (npx expo run:ios).');
      return;
    }
    setScanning(true);
    try {
      const { parsed, text } = await readReceipt(business.id, picked);
      setOcrText(text);
      const name = parsed.vendor?.trim();
      if (name) setVendor(name);
      if (parsed.date) setDate(parseDate(parsed.date));
      if (parsed.total != null) setTotal(String(parsed.total));
      if (parsed.items.length) {
        setItems(parsed.items.map((i) => ({ id: uuid(), description: i.description, amount: String(i.amount) })));
      }
      // Reuse the category this vendor usually gets, unless one is already picked.
      if (name && !categoryId) {
        api.vendorCategory(business.id, name).then((r) => r.categoryId && setCategoryId(r.categoryId)).catch(() => {});
      }
      setScanNote(parsed.total == null && !parsed.items.length
        ? "Couldn't find amounts on this receipt. Fill them in below."
        : 'Filled in from the receipt. Check the numbers before saving.');
    } catch (e) {
      setScanNote(`${e instanceof Error ? e.message : String(e)} The photo will still be saved.`);
    } finally {
      setScanning(false);
    }
  }

  function removePhoto() {
    setPhoto(undefined);
    setReceiptPath(undefined);
    setSavedPhotoUrl(undefined);
    setOcrText(undefined);
    setScanNote(undefined);
  }

  const photoUri = photo?.uri ?? savedPhotoUrl;

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
      const path = photo ? await uploadReceipt(business.id, id, photo) : receiptPath;
      await api.saveExpense(business.id, {
        id,
        receiptPath: path,
        ocrRawText: ocrText,
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
      { text: 'Delete', style: 'destructive', onPress: async () => { await api.deleteExpense(business.id, id); router.back(); } },
    ]);
  }

  return (
    <Sheet title={editing ? 'Edit expense' : 'Add expense'} onClose={() => router.back()}>
      {!loaded ? <Loading /> : (
        <>
          <Section title="Receipt">
            {photoUri ? (
              <View style={{ flexDirection: 'row', gap: S.md, alignItems: 'center' }}>
                <Pressable onPress={() => setViewing(true)} accessibilityLabel="View receipt photo">
                  <Image source={{ uri: photoUri }} style={{ width: 72, height: 96, borderRadius: R.sm, backgroundColor: C.line }} />
                </Pressable>
                <View style={{ flex: 1, gap: S.xs }}>
                  {scanning ? (
                    <View style={{ flexDirection: 'row', alignItems: 'center', gap: S.sm }}>
                      <ActivityIndicator color={C.toner} />
                      <Txt kind="small" color={C.inkSoft}>Reading receipt...</Txt>
                    </View>
                  ) : (
                    <>
                      <Button kind="quiet" icon="arrow.triangle.2.circlepath.camera" title="Replace photo" onPress={() => scan('library')} />
                      <Button kind="quiet" icon="trash" title="Remove photo" onPress={removePhoto} />
                    </>
                  )}
                </View>
              </View>
            ) : (
              <View style={{ flexDirection: 'row', gap: S.sm }}>
                <Button kind="secondary" icon="camera" title="Take photo" onPress={() => scan('camera')} style={{ flex: 1 }} />
                <Button kind="secondary" icon="photo" title="Choose photo" onPress={() => scan('library')} style={{ flex: 1 }} />
              </View>
            )}
            {scanNote && <Txt kind="small" color={C.inkSoft}>{scanNote}</Txt>}
          </Section>

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
          <Button title={editing ? 'Save changes' : 'Save expense'} onPress={save} loading={busy} disabled={scanning} />
          {editing && <Button kind="danger" title="Delete expense" onPress={remove} />}

          <Modal visible={viewing && !!photoUri} animationType="fade" onRequestClose={() => setViewing(false)}>
            <Pressable style={{ flex: 1, backgroundColor: '#000' }} onPress={() => setViewing(false)} accessibilityLabel="Close photo">
              {photoUri && <Image source={{ uri: photoUri }} style={{ flex: 1 }} resizeMode="contain" />}
            </Pressable>
          </Modal>
        </>
      )}
    </Sheet>
  );
}
