const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const usdWhole = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });

export const money = (n: number | null | undefined) => usd.format(n ?? 0);
export const moneyShort = (n: number | null | undefined) => usdWhole.format(n ?? 0);

const pad = (n: number) => String(n).padStart(2, '0');

/** Local calendar date as YYYY-MM-DD (never UTC, so 9pm doesn't become tomorrow). */
export function isoDate(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function parseDate(s: string): Date {
  const [y, m, d] = s.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function addDays(d: Date, n: number): Date {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
}

export function startOfWeek(d: Date): Date {
  const x = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const offset = (x.getDay() + 6) % 7; // Monday start
  return addDays(x, -offset);
}

export function sameDay(a: Date, b: Date): boolean {
  return isoDate(a) === isoDate(b);
}

export const dayTitle = (d: Date) =>
  d.toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' });

export const shortDate = (s: string) =>
  parseDate(s).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });

export const longDate = (s: string) =>
  parseDate(s).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });

export const time = (iso: string) =>
  new Date(iso).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });

export const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August',
  'September', 'October', 'November', 'December'];
export const MONTHS_SHORT = MONTHS.map((m) => m.slice(0, 3));

export function pct(n: number | null | undefined): string {
  if (n == null) return 'no data';
  const r = Math.round(n);
  return `${r > 0 ? '+' : ''}${r}%`;
}

/** Parses what someone typed into a money field. Empty or junk returns null. */
export function parseMoney(s: string): number | null {
  const n = Number(s.replace(/[$,\s]/g, ''));
  return s.trim() === '' || Number.isNaN(n) ? null : Math.round(n * 100) / 100;
}
