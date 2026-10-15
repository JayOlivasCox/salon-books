import { SymbolView, type SymbolViewProps } from 'expo-symbols';
import type { ReactNode } from 'react';
import {
  ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View,
  type StyleProp, type TextInputProps, type TextStyle, type ViewStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { money } from '@/lib/format';
import { C, R, S, T } from '@/lib/theme';

export function Screen({ children, scroll = true, style }: { children: ReactNode; scroll?: boolean; style?: StyleProp<ViewStyle> }) {
  return (
    <SafeAreaView edges={['top']} style={styles.screen}>
      {scroll ? (
        <ScrollView contentContainerStyle={[styles.screenPad, style]} keyboardShouldPersistTaps="handled">
          {children}
        </ScrollView>
      ) : (
        <View style={[styles.screenPad, { flex: 1 }, style]}>{children}</View>
      )}
    </SafeAreaView>
  );
}

/** Modal sheet layout: title row with a close action, then content. */
export function Sheet({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <View style={styles.screen}>
      <View style={styles.sheetHeader}>
        <Text style={[T.heading, { color: C.ink }]}>{title}</Text>
        <Pressable onPress={onClose} hitSlop={12} accessibilityRole="button">
          <Text style={[T.label, { color: C.toner }]}>Close</Text>
        </Pressable>
      </View>
      <ScrollView contentContainerStyle={styles.sheetBody} keyboardShouldPersistTaps="handled">
        {children}
      </ScrollView>
    </View>
  );
}

export function Txt({ children, style, kind = 'body', color = C.ink, numberOfLines }: {
  children: ReactNode; style?: StyleProp<TextStyle>; kind?: keyof typeof T; color?: string; numberOfLines?: number;
}) {
  return <Text numberOfLines={numberOfLines} style={[T[kind], { color }, style]}>{children}</Text>;
}

export function Money({ value, size = 16, color = C.ink, style }: {
  value: number | null | undefined; size?: number; color?: string; style?: StyleProp<TextStyle>;
}) {
  return <Text style={[T.money, { fontSize: size, color }, style]}>{money(value)}</Text>;
}

export function Button({ title, onPress, kind = 'primary', loading, disabled, icon, style }: {
  title: string; onPress: () => void; kind?: 'primary' | 'secondary' | 'danger' | 'quiet';
  loading?: boolean; disabled?: boolean; icon?: SymbolViewProps['name']; style?: StyleProp<ViewStyle>;
}) {
  const fg = kind === 'primary' ? '#fff' : kind === 'danger' ? C.expense : C.toner;
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed }) => [
        styles.btn,
        kind === 'primary' && { backgroundColor: C.toner },
        kind === 'secondary' && { backgroundColor: C.tonerSoft },
        kind === 'danger' && { backgroundColor: C.expenseSoft },
        kind === 'quiet' && { backgroundColor: 'transparent', paddingHorizontal: 0 },
        (pressed || disabled) && { opacity: 0.6 },
        style,
      ]}>
      {loading ? <ActivityIndicator color={fg} /> : (
        <View style={styles.btnInner}>
          {icon && <SymbolView name={icon} size={17} tintColor={fg} />}
          <Text style={[T.label, { color: fg, fontSize: 16 }]}>{title}</Text>
        </View>
      )}
    </Pressable>
  );
}

export function Field({ label, style, ...input }: TextInputProps & { label: string }) {
  return (
    <View style={{ gap: S.xs }}>
      <Txt kind="label" color={C.inkSoft}>{label}</Txt>
      <TextInput placeholderTextColor={C.inkFaint} style={[styles.input, style]} {...input} />
    </View>
  );
}

export function Chip({ label, selected, onPress, dot }: { label: string; selected?: boolean; onPress: () => void; dot?: string }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ selected }}
      style={[styles.chip, selected && { backgroundColor: C.ink, borderColor: C.ink }]}>
      {dot && <View style={[styles.dot, { backgroundColor: dot }]} />}
      <Text style={[T.label, { color: selected ? '#fff' : C.ink }]}>{label}</Text>
    </Pressable>
  );
}

export function ChipRow({ children, wrap }: { children: ReactNode; wrap?: boolean }) {
  if (wrap) return <View style={styles.chipWrap}>{children}</View>;
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: S.sm }}>
      {children}
    </ScrollView>
  );
}

export function Segmented<V extends string>({ options, value, onChange }: {
  options: { value: V; label: string }[]; value: V; onChange: (v: V) => void;
}) {
  return (
    <View style={styles.segment}>
      {options.map((o) => (
        <Pressable
          key={o.value}
          onPress={() => onChange(o.value)}
          accessibilityRole="button"
          accessibilityState={{ selected: o.value === value }}
          style={[styles.segmentItem, o.value === value && styles.segmentOn]}>
          <Text style={[T.label, { color: o.value === value ? C.ink : C.inkSoft }]}>{o.label}</Text>
        </Pressable>
      ))}
    </View>
  );
}

/** "‹ October 2026 ›" style period switcher. */
export function Stepper({ label, onPrev, onNext, onReset }: { label: string; onPrev: () => void; onNext: () => void; onReset?: () => void }) {
  return (
    <View style={styles.stepper}>
      <Pressable onPress={onPrev} hitSlop={12} accessibilityLabel="Previous">
        <SymbolView name="chevron.left" size={18} tintColor={C.toner} />
      </Pressable>
      <Pressable onPress={onReset} disabled={!onReset} style={{ flex: 1 }}>
        <Txt kind="heading" style={{ textAlign: 'center' }}>{label}</Txt>
      </Pressable>
      <Pressable onPress={onNext} hitSlop={12} accessibilityLabel="Next">
        <SymbolView name="chevron.right" size={18} tintColor={C.toner} />
      </Pressable>
    </View>
  );
}

export function Section({ title, action, children }: { title?: string; action?: ReactNode; children: ReactNode }) {
  return (
    <View style={{ gap: S.sm }}>
      {(title || action) && (
        <View style={styles.sectionHead}>
          {title && <Txt kind="heading">{title}</Txt>}
          {action}
        </View>
      )}
      {children}
    </View>
  );
}

export function Panel({ children, style }: { children: ReactNode; style?: StyleProp<ViewStyle> }) {
  return <View style={[styles.panel, style]}>{children}</View>;
}

export function Stat({ label, value, color = C.ink, size = 22 }: { label: string; value: number; color?: string; size?: number }) {
  return (
    <View style={{ gap: 2, flex: 1 }}>
      <Txt kind="small" color={C.inkSoft}>{label}</Txt>
      <Money value={value} size={size} color={color} />
    </View>
  );
}

export function ErrorNote({ message }: { message?: string }) {
  if (!message) return null;
  return (
    <View style={styles.error}>
      <Txt kind="small" color={C.expense}>{message}</Txt>
    </View>
  );
}

export function Loading() {
  return <ActivityIndicator style={{ marginTop: S.xl }} color={C.toner} />;
}

export const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: C.bg },
  screenPad: { padding: S.lg, gap: S.lg, paddingBottom: S.xxl * 2 },
  sheetHeader: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingHorizontal: S.lg, paddingTop: S.xl, paddingBottom: S.md,
  },
  sheetBody: { padding: S.lg, gap: S.lg, paddingBottom: S.xxl * 2 },
  btn: { minHeight: 50, borderRadius: R.md, alignItems: 'center', justifyContent: 'center', paddingHorizontal: S.lg },
  btnInner: { flexDirection: 'row', alignItems: 'center', gap: S.sm },
  input: {
    backgroundColor: C.surface, borderRadius: R.sm, borderWidth: 1, borderColor: C.line,
    paddingHorizontal: S.md, paddingVertical: S.md, fontSize: 16, color: C.ink,
  },
  chip: {
    flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 14, paddingVertical: 8,
    borderRadius: R.pill, borderWidth: 1, borderColor: C.line, backgroundColor: C.surface,
  },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: S.sm },
  dot: { width: 9, height: 9, borderRadius: 5 },
  segment: { flexDirection: 'row', backgroundColor: C.line, borderRadius: R.sm, padding: 3 },
  segmentItem: { flex: 1, paddingVertical: 8, alignItems: 'center', borderRadius: 6 },
  segmentOn: { backgroundColor: C.surface },
  stepper: { flexDirection: 'row', alignItems: 'center', gap: S.md },
  sectionHead: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  panel: { backgroundColor: C.surface, borderRadius: R.md, padding: S.lg, gap: S.md },
  error: { backgroundColor: C.expenseSoft, padding: S.md, borderRadius: R.sm },
});
