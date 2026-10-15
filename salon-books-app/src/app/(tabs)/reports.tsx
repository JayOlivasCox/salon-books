import * as Sharing from 'expo-sharing';
import { useState } from 'react';
import { View } from 'react-native';

import { BarChart, Donut } from '@/components/charts';
import { Button, ErrorNote, Loading, Money, Panel, Screen, Section, Segmented, Stat, Stepper, Txt } from '@/components/ui';
import { useCurrentBusiness } from '@/context/business';
import { api } from '@/lib/api';
import { addDays, isoDate, MONTHS, MONTHS_SHORT, parseDate, pct, startOfWeek } from '@/lib/format';
import { C, S } from '@/lib/theme';
import type { PeriodSummary } from '@/lib/types';
import { useLoad } from '@/lib/use-load';

type Period = 'week' | 'month' | 'year';

export default function Reports() {
  const [period, setPeriod] = useState<Period>('month');
  return (
    <Screen>
      <Txt kind="title">Reports</Txt>
      <Segmented value={period} onChange={setPeriod}
        options={[{ value: 'week', label: 'Week' }, { value: 'month', label: 'Month' }, { value: 'year', label: 'Year' }]} />
      {period === 'week' && <Week />}
      {period === 'month' && <Month />}
      {period === 'year' && <Year />}
    </Screen>
  );
}

function Totals({ s, showRent }: { s: PeriodSummary; showRent: boolean }) {
  return (
    <Panel>
      <View style={{ flexDirection: 'row', gap: S.md }}>
        <Stat label="Income" value={s.income.gross} color={C.income} />
        <Stat label="Expenses" value={s.totalExpenses} color={C.expense} />
        <Stat label="Net" value={s.net} />
      </View>
      <View style={{ gap: 4 }}>
        <Line label="Services" value={s.income.services} />
        <Line label="Tips" value={s.income.tips} />
        <Line label="Supplies and other expenses" value={s.expenses} />
        {showRent && <Line label="Booth rent" value={s.rent} />}
      </View>
    </Panel>
  );
}

function Line({ label, value, color = C.inkSoft }: { label: string; value: number; color?: string }) {
  return (
    <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
      <Txt kind="small" color={color}>{label}</Txt>
      <Money value={value} size={14} color={color} />
    </View>
  );
}

function Week() {
  const { business } = useCurrentBusiness();
  const [start, setStart] = useState(() => startOfWeek(new Date()));
  const key = isoDate(start);
  const { data, error } = useLoad(() => api.weekly(business.id, key), [business.id, key]);
  const end = addDays(start, 6);
  const label = `${start.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} to ${end.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}`;

  return (
    <>
      <Stepper label={label} onPrev={() => setStart(addDays(start, -7))} onNext={() => setStart(addDays(start, 7))}
        onReset={() => setStart(startOfWeek(new Date()))} />
      <ErrorNote message={error} />
      {!data ? <Loading /> : (
        <>
          <Totals s={data.summary} showRent={data.showRent} />
          <Section title="Made each day">
            <Panel>
              <BarChart
                values={data.days.map((d) => d.gross)}
                labels={data.days.map((d) => parseDate(d.date).toLocaleDateString('en-US', { weekday: 'narrow' }))}
                color={C.income}
              />
            </Panel>
          </Section>
        </>
      )}
    </>
  );
}

function Month() {
  const { business } = useCurrentBusiness();
  const now = new Date();
  const [ym, setYm] = useState({ y: now.getFullYear(), m: now.getMonth() + 1 });
  const { data, error } = useLoad(() => api.monthly(business.id, ym.y, ym.m), [business.id, ym.y, ym.m]);
  const shift = (n: number) => {
    const d = new Date(ym.y, ym.m - 1 + n, 1);
    setYm({ y: d.getFullYear(), m: d.getMonth() + 1 });
  };
  const prevName = MONTHS[(ym.m + 10) % 12];

  return (
    <>
      <Stepper label={`${MONTHS[ym.m - 1]} ${ym.y}`} onPrev={() => shift(-1)} onNext={() => shift(1)}
        onReset={() => setYm({ y: now.getFullYear(), m: now.getMonth() + 1 })} />
      <ErrorNote message={error} />
      {!data ? <Loading /> : (
        <>
          <Totals s={data.summary} showRent={data.showRent} />

          <Section title="Compared to">
            <Panel>
              <Line label={`${prevName}: income ${pct(data.previousMonth.grossChangePct)}`} value={data.previousMonth.gross} />
              <Line label={`${MONTHS_SHORT[ym.m - 1]} ${ym.y - 1}: income ${pct(data.sameMonthLastYear.grossChangePct)}`}
                value={data.sameMonthLastYear.gross} />
              <Line label={`${prevName}: net ${pct(data.previousMonth.netChangePct)}`} value={data.previousMonth.net} />
            </Panel>
          </Section>

          <Section title="Where the money went">
            <Panel>
              {data.summary.byCategory.length === 0 && !data.showRent ? (
                <Txt kind="small" color={C.inkSoft}>No expenses this month.</Txt>
              ) : (
                <View style={{ flexDirection: 'row', gap: S.lg, alignItems: 'center' }}>
                  <Donut slices={[
                    ...data.summary.byCategory.map((c) => ({ value: c.amount, color: c.color ?? C.inkFaint })),
                    ...(data.showRent && data.summary.rent > 0 ? [{ value: data.summary.rent, color: C.chair }] : []),
                  ]} size={130} />
                  <View style={{ flex: 1, gap: 6 }}>
                    {data.summary.byCategory.map((c) => (
                      <Legend key={c.categoryId ?? c.name} color={c.color ?? C.inkFaint} label={c.name} value={c.amount} />
                    ))}
                    {data.showRent && data.summary.rent > 0 && <Legend color={C.chair} label="Booth rent" value={data.summary.rent} />}
                  </View>
                </View>
              )}
            </Panel>
          </Section>

          {data.summary.topVendors.length > 0 && (
            <Section title="Top vendors">
              <Panel>
                {data.summary.topVendors.map((v) => <Line key={v.vendor} label={`${v.vendor} (${v.count})`} value={v.amount} color={C.ink} />)}
              </Panel>
            </Section>
          )}
        </>
      )}
    </>
  );
}

function Legend({ color, label, value }: { color: string; label: string; value: number }) {
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
      <View style={{ width: 9, height: 9, borderRadius: 5, backgroundColor: color }} />
      <Txt kind="small" style={{ flex: 1 }} numberOfLines={1}>{label}</Txt>
      <Money value={value} size={13} />
    </View>
  );
}

function Year() {
  const { business } = useCurrentBusiness();
  const thisYear = new Date().getFullYear();
  const [year, setYear] = useState(thisYear);
  const { data, error } = useLoad(() => api.yearly(business.id, year), [business.id, year]);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState<string>();

  async function exportPdf() {
    setExporting(true);
    setExportError(undefined);
    try {
      const file = await api.downloadTaxPdf(business.id, year);
      await Sharing.shareAsync(file.uri, { mimeType: 'application/pdf', UTI: 'com.adobe.pdf' });
    } catch (e) {
      setExportError(e instanceof Error ? e.message : String(e));
    } finally {
      setExporting(false);
    }
  }

  const p = data?.projectionNextYear;
  return (
    <>
      <Stepper label={String(year)} onPrev={() => setYear(year - 1)} onNext={() => setYear(year + 1)}
        onReset={() => setYear(thisYear)} />
      <ErrorNote message={error} />
      {!data || !p ? <Loading /> : (
        <>
          <Totals s={data.summary} showRent />

          <Section title="Income by month">
            <Panel>
              <BarChart values={data.months.map((m) => m.gross)} labels={MONTHS_SHORT.map((m) => m[0])}
                highlight={data.busiestMonth ? data.busiestMonth.month - 1 : undefined} />
              {data.busiestMonth && data.slowestMonth && (
                <Txt kind="small" color={C.inkSoft}>
                  Busiest: {MONTHS[data.busiestMonth.month - 1]}. Slowest: {MONTHS[data.slowestMonth.month - 1]}.
                </Txt>
              )}
            </Panel>
          </Section>

          <Section title={`${year + 1} projection`}>
            <Panel>
              {p.method === 'INSUFFICIENT_DATA' ? (
                <Txt kind="small" color={C.inkSoft}>Log at least three full months to see a projection.</Txt>
              ) : (
                <>
                  <View style={{ flexDirection: 'row', gap: S.md }}>
                    <Stat label="Income" value={p.gross} color={C.income} size={18} />
                    <Stat label="Expenses" value={p.expenses} color={C.expense} size={18} />
                    <Stat label="Net" value={p.net} size={18} />
                  </View>
                  <BarChart values={p.grossByMonth} labels={MONTHS_SHORT.map((m) => m[0])} height={120} color={C.toner} />
                  <Txt kind="small" color={C.inkSoft}>
                    {p.method === 'YOY_GROWTH'
                      ? `Based on ${p.growthRate >= 0 ? 'growth' : 'decline'} of ${Math.abs(Math.round(p.growthRate * 100))}% from last year, keeping your busy and slow months.`
                      : 'Based on your trend so far this year.'}
                  </Txt>
                </>
              )}
            </Panel>
          </Section>

          <Section title="Taxes">
            <Panel>
              <Txt kind="small" color={C.inkSoft}>
                A Schedule C summary with expenses grouped by tax line, ready for you or your accountant.
              </Txt>
              <ErrorNote message={exportError} />
              <Button icon="square.and.arrow.up" title={`Export ${year} tax PDF`} onPress={exportPdf} loading={exporting} />
            </Panel>
          </Section>
        </>
      )}
    </>
  );
}
