import { router } from 'expo-router';
import { useState } from 'react';
import { Alert, View } from 'react-native';

import { Button, Chip, ChipRow, ErrorNote, Field, Panel, Screen, Section, Segmented, Txt } from '@/components/ui';
import { useCurrentBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { isoDate, longDate, money } from '@/lib/format';
import { supabase } from '@/lib/supabase';
import { C, S } from '@/lib/theme';
import type { ExpenseSort, RentDisplay } from '@/lib/types';
import { useLoad } from '@/lib/use-load';

export default function Settings() {
  const { business, me, services, setBusiness, refresh } = useCurrentBusiness();
  const [name, setName] = useState(business.name);
  const [error, setError] = useState<string>();

  const leases = useLoad(() => Promise.all([api.leases(business.id), api.expiringLeases(business.id)]), [business.id], { cache: 'leases' });
  const members = useLoad(() => api.members(business.id), [business.id], { cache: 'members' });

  async function update(patch: { name?: string; rentDisplay?: RentDisplay; defaultExpenseSort?: ExpenseSort }) {
    try {
      setBusiness(await api.updateSettings(business.id, patch));
      setError(undefined);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  const [allLeases, expiring] = leases.data ?? [[], []];
  const today = isoDate(new Date());
  const current = allLeases.find((l) => l.startDate <= today && l.endDate >= today);
  const isOwner = business.role === 'OWNER';

  function removePartner(userId: string, label: string) {
    Alert.alert(`Remove ${label}?`, 'They will lose access to these books.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Remove', style: 'destructive', onPress: async () => {
          await api.removeMember(business.id, userId);
          if (userId === me) await refresh();
          else members.reload();
        } },
    ]);
  }

  return (
    <Screen>
      <Txt kind="title">Settings</Txt>
      <ErrorNote message={error} />

      <Section title="Business">
        <Panel>
          <Field label="Name" value={name} onChangeText={setName} onEndEditing={() => name.trim() && name !== business.name && update({ name: name.trim() })} />
        </Panel>
      </Section>

      <Section title="Booth rent">
        <Panel>
          {current ? (
            <Txt>
              {money(current.amount)} {current.frequency === 'MONTHLY' ? 'a month' : 'a week'}, until {longDate(current.endDate)}
            </Txt>
          ) : (
            <Txt color={C.inkSoft}>No booth rent set. Add it so reports count it automatically.</Txt>
          )}
          {expiring.length > 0 && (
            <Txt kind="small" color={C.expense}>Your lease ends {longDate(expiring[0].endDate)}. Update it if you renewed.</Txt>
          )}
          <Button kind="secondary" title={current ? 'Change booth rent' : 'Set booth rent'}
            onPress={() => router.push({ pathname: '/lease', params: current ? { id: current.id } : {} })} />
          <View style={{ gap: S.xs }}>
            <Txt kind="label" color={C.inkSoft}>Show rent on</Txt>
            <Segmented value={business.rentDisplay} onChange={(v) => update({ rentDisplay: v })}
              options={[{ value: 'WEEKLY', label: 'Weekly report' }, { value: 'MONTHLY', label: 'Monthly report' }]} />
          </View>
        </Panel>
      </Section>

      <Section title="Expenses">
        <Panel>
          <Txt kind="label" color={C.inkSoft}>Sort expenses by</Txt>
          <ChipRow wrap>
            {(['DATE', 'CATEGORY', 'VENDOR', 'AMOUNT'] as ExpenseSort[]).map((s) => (
              <Chip key={s} label={s[0] + s.slice(1).toLowerCase()} selected={business.defaultExpenseSort === s}
                onPress={() => update({ defaultExpenseSort: s })} />
            ))}
          </ChipRow>
        </Panel>
      </Section>

      <Section title="Services">
        <Panel>
          <Txt color={services.length ? C.ink : C.inkSoft}>
            {services.length ? services.map((s) => s.name).join(', ') : 'No services yet.'}
          </Txt>
          <Button kind="secondary" title="Edit services" onPress={() => router.push('/services')} />
        </Panel>
      </Section>

      <Section title="Partners">
        <Panel>
          {members.data?.map((m) => (
            <View key={m.userId} style={{ flexDirection: 'row', alignItems: 'center', gap: S.sm }}>
              <View style={{ flex: 1 }}>
                <Txt kind="label">{m.displayName ?? m.email ?? 'Partner'}{m.userId === me ? ' (you)' : ''}</Txt>
                <Txt kind="small" color={C.inkSoft}>{m.role === 'OWNER' ? 'Owner' : 'Partner'}</Txt>
              </View>
              {isOwner && m.userId !== me && (
                <Button kind="quiet" title="Remove" onPress={() => removePartner(m.userId, m.email ?? 'partner')} />
              )}
            </View>
          ))}
          {isOwner && <Button kind="secondary" icon="qrcode" title="Invite partner" onPress={() => router.push('/invite')} />}
          {!isOwner && <Button kind="quiet" title="Leave this business" onPress={() => removePartner(me, 'yourself')} />}
        </Panel>
      </Section>

      <Button kind="danger" title="Sign out" onPress={() => supabase.auth.signOut()} />
    </Screen>
  );
}
