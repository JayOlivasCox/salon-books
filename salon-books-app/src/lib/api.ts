import { File, Paths } from 'expo-file-system';

import { supabase } from './supabase';
import type {
  AppointmentView, Business, Category, Client, DailyReport, Expense, ExpenseSort, IncomeEntry, Lease, Member,
  MonthlyReport, Page, PriceSuggestion, RentDisplay, ServiceType, WeeklyReport, YearlyReport,
} from './types';

/** Simulator: localhost works. Real iPhone: use your Mac's local IP, e.g. http://192.168.1.20:8080 */
export const API_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function token(): Promise<string> {
  const { data } = await supabase.auth.getSession();
  if (!data.session) throw new ApiError(401, 'You are signed out. Sign in again.');
  return data.session.access_token;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  let res: Response;
  try {
    res = await fetch(API_URL + path, {
      method,
      headers: { Authorization: `Bearer ${await token()}`, 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (e) {
    if (e instanceof ApiError) throw e;
    throw new ApiError(0, `Can't reach the server at ${API_URL}. Is the backend running?`);
  }
  const text = await res.text();
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const j = JSON.parse(text);
      if (j.detail) message = j.detail;
    } catch {
      // not JSON
    }
    throw new ApiError(res.status, message);
  }
  return (text ? JSON.parse(text) : undefined) as T;
}

const get = <T>(p: string) => request<T>('GET', p);
const put = <T>(p: string, b: unknown) => request<T>('PUT', p, b);
const post = <T>(p: string, b?: unknown) => request<T>('POST', p, b ?? {});
const patch = <T>(p: string, b: unknown) => request<T>('PATCH', p, b);
const del = (p: string) => request<void>('DELETE', p);

const qs = (params: Record<string, string | number | undefined | null>) => {
  const parts = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`);
  return parts.length ? `?${parts.join('&')}` : '';
};

const now = () => new Date().toISOString();

export const api = {
  me: () => get<{ profile: { id: string; email?: string }; businesses: Business[] }>('/api/me'),

  createBusiness: (name: string, timezone: string) => post<Business>('/api/businesses', { name, timezone }),
  updateSettings: (b: string, s: { name?: string; rentDisplay?: RentDisplay; defaultExpenseSort?: ExpenseSort }) =>
    patch<Business>(`/api/businesses/${b}/settings`, s),
  members: (b: string) => get<Member[]>(`/api/businesses/${b}/members`),
  removeMember: (b: string, userId: string) => del(`/api/businesses/${b}/members/${userId}`),
  createInvite: (b: string) => post<{ token: string; deepLink: string; expiresAt: string }>(`/api/businesses/${b}/invites`),
  acceptInvite: (t: string) => post<Business>('/api/invites/accept', { token: t }),

  categories: (b: string) => get<Category[]>(`/api/businesses/${b}/categories`),
  clients: (b: string) => get<Client[]>(`/api/businesses/${b}/clients`),
  saveClient: (b: string, c: { id: string; name: string }) =>
    put<Client>(`/api/businesses/${b}/clients/${c.id}`, { ...c, clientModifiedAt: now() }),
  services: (b: string) => get<ServiceType[]>(`/api/businesses/${b}/service-types`),
  saveService: (b: string, s: { id: string; name: string; defaultDurationMinutes?: number }) =>
    put<ServiceType>(`/api/businesses/${b}/service-types/${s.id}`, { ...s, clientModifiedAt: now() }),
  deleteService: (b: string, id: string) => del(`/api/businesses/${b}/service-types/${id}`),

  saveAppointment: (b: string, a: {
    id: string; stylistId: string; clientId?: string; clientLabel?: string; serviceTypeId?: string;
    startsAt: string; endsAt: string;
  }) => put<AppointmentView>(`/api/businesses/${b}/appointments/${a.id}`, { ...a, clientModifiedAt: now() }),
  deleteAppointment: (b: string, id: string) => del(`/api/businesses/${b}/appointments/${id}`),

  saveIncome: (b: string, e: {
    id: string; stylistId: string; appointmentId?: string; clientId?: string; serviceTypeId?: string;
    serviceDate: string; serviceAmount: number; tipAmount: number; paymentMethod?: string;
  }) => put<IncomeEntry>(`/api/businesses/${b}/income/${e.id}`, { ...e, clientModifiedAt: now() }),
  deleteIncome: (b: string, id: string) => del(`/api/businesses/${b}/income/${id}`),
  suggestion: (b: string, clientId?: string, serviceTypeId?: string) =>
    get<PriceSuggestion>(`/api/businesses/${b}/income/suggestion${qs({ clientId, serviceTypeId })}`),

  expenses: (b: string, f: {
    from?: string; to?: string; categoryId?: string; vendor?: string; sort?: ExpenseSort; dir?: 'asc' | 'desc'; size?: number;
  }) => get<Page<Expense>>(`/api/businesses/${b}/expenses${qs({ ...f, size: f.size ?? 200 })}`),
  expense: (b: string, id: string) => get<Expense>(`/api/businesses/${b}/expenses/${id}`),
  saveExpense: (b: string, e: {
    id: string; expenseDate: string; vendor?: string; total: number; categoryId?: string; notes?: string;
    receiptPath?: string; items: { id?: string; description: string; amount: number; categoryId?: string }[];
  }) => put<Expense>(`/api/businesses/${b}/expenses/${e.id}`, { ...e, clientModifiedAt: now() }),
  deleteExpense: (b: string, id: string) => del(`/api/businesses/${b}/expenses/${id}`),
  vendors: (b: string) => get<string[]>(`/api/businesses/${b}/expenses/vendors`),

  leases: (b: string) => get<Lease[]>(`/api/businesses/${b}/leases`),
  expiringLeases: (b: string) => get<Lease[]>(`/api/businesses/${b}/leases/expiring`),
  saveLease: (b: string, l: {
    id: string; amount: number; frequency: string; startDate: string; endDate?: string; durationMonths?: number;
  }) => put<Lease>(`/api/businesses/${b}/leases/${l.id}`, { ...l, clientModifiedAt: now() }),
  deleteLease: (b: string, id: string) => del(`/api/businesses/${b}/leases/${id}`),

  daily: (b: string, date: string) => get<DailyReport>(`/api/businesses/${b}/reports/daily${qs({ date })}`),
  weekly: (b: string, start: string) => get<WeeklyReport>(`/api/businesses/${b}/reports/weekly${qs({ start })}`),
  monthly: (b: string, year: number, month: number) =>
    get<MonthlyReport>(`/api/businesses/${b}/reports/monthly${qs({ year, month })}`),
  yearly: (b: string, year: number) => get<YearlyReport>(`/api/businesses/${b}/reports/yearly${qs({ year })}`),

  /** Downloads the Schedule C PDF into the cache folder and returns the local file. */
  async downloadTaxPdf(b: string, year: number): Promise<File> {
    const dest = new File(Paths.cache, `schedule-c-summary-${year}.pdf`);
    if (dest.exists) dest.delete();
    return File.downloadFileAsync(`${API_URL}/api/businesses/${b}/reports/tax-pdf?year=${year}`, dest, {
      headers: { Authorization: `Bearer ${await token()}` },
    });
  },
};
