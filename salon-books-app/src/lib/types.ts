export type RentDisplay = 'WEEKLY' | 'MONTHLY';
export type ExpenseSort = 'DATE' | 'CATEGORY' | 'VENDOR' | 'AMOUNT';
export type Role = 'OWNER' | 'PARTNER';
export type PaymentMethod = 'CASH' | 'CARD' | 'VENMO' | 'ZELLE' | 'CASH_APP' | 'OTHER';
export type RentFrequency = 'WEEKLY' | 'MONTHLY';
export type ScheduleCLine = string;

export interface Business {
  id: string;
  name: string;
  timezone: string;
  rentDisplay: RentDisplay;
  defaultExpenseSort: ExpenseSort;
  role: Role;
}

export interface Synced {
  id: string;
  businessId: string;
  updatedAt: string;
  clientModifiedAt: string;
  deletedAt?: string;
}

export interface Category extends Synced {
  name: string;
  scheduleCLine: ScheduleCLine;
  color?: string;
  defaultCategory: boolean;
  sortOrder: number;
}

export interface Client extends Synced {
  name: string;
  phone?: string;
  email?: string;
  notes?: string;
}

export interface ServiceType extends Synced {
  name: string;
  defaultDurationMinutes?: number;
}

export interface IncomeEntry extends Synced {
  stylistId: string;
  appointmentId?: string;
  clientId?: string;
  serviceTypeId?: string;
  serviceDate: string;
  serviceAmount: number;
  tipAmount: number;
  paymentMethod?: PaymentMethod;
  notes?: string;
}

export interface ExpenseItem {
  id: string;
  description: string;
  amount: number;
  categoryId?: string;
}

export interface Expense extends Synced {
  expenseDate: string;
  vendor?: string;
  total: number;
  categoryId?: string;
  itemized: boolean;
  receiptPath?: string;
  notes?: string;
  items: ExpenseItem[];
}

export interface Lease extends Synced {
  label?: string;
  amount: number;
  frequency: RentFrequency;
  startDate: string;
  endDate: string;
  notes?: string;
}

export interface Member {
  userId: string;
  email?: string;
  displayName?: string;
  role: Role;
  joinedAt: string;
}

export interface IncomeTotals { services: number; tips: number; gross: number; entries: number }
export interface CategoryTotal { categoryId?: string; name: string; color?: string; scheduleCLine: string; amount: number }
export interface VendorTotal { vendor: string; amount: number; count: number }
export interface PeriodSummary {
  from: string; to: string; income: IncomeTotals; expenses: number; rent: number;
  totalExpenses: number; net: number; byCategory: CategoryTotal[]; topVendors: VendorTotal[];
}

export interface AppointmentView {
  id: string; startsAt: string; endsAt: string; stylistId: string; clientId?: string;
  clientName?: string; serviceTypeId?: string; serviceName?: string; status: string; incomeEntryId?: string;
}
export interface DailyReport {
  date: string; appointments: AppointmentView[]; bookedCount: number; unnamedCount: number;
  entries: IncomeEntry[]; totals: IncomeTotals;
}
export interface DayTotal { date: string; services: number; tips: number; gross: number }
export interface WeeklyReport { weekStart: string; summary: PeriodSummary; showRent: boolean; days: DayTotal[] }
export interface Comparison {
  from: string; to: string; gross: number; totalExpenses: number; net: number;
  grossChangePct?: number; netChangePct?: number;
}
export interface MonthlyReport {
  year: number; month: number; summary: PeriodSummary; showRent: boolean;
  previousMonth: Comparison; sameMonthLastYear: Comparison;
}
export interface MonthRow { month: number; services: number; tips: number; gross: number; expenses: number; rent: number; net: number }
export interface MonthStat { month: number; gross: number }
export interface ProjectionView {
  method: 'YOY_GROWTH' | 'LINEAR_TREND' | 'INSUFFICIENT_DATA'; growthRate: number;
  grossByMonth: number[]; expensesByMonth: number[]; gross: number; expenses: number; net: number;
}
export interface YearlyReport {
  year: number; summary: PeriodSummary; months: MonthRow[];
  busiestMonth?: MonthStat; slowestMonth?: MonthStat; projectionNextYear: ProjectionView;
}

export interface ServiceHistory {
  serviceTypeId?: string; serviceName?: string; lastAmount: number; lastTip: number; lastDate: string; timesDone: number;
}
export interface PriceSuggestion {
  clientId?: string; serviceTypeId?: string; suggestedAmount?: number; suggestedTip?: number;
  source: 'CLIENT_LAST' | 'SERVICE_MEDIAN' | 'NONE'; typicalAmount?: number; typicalSampleSize: number;
  clientHistory: ServiceHistory[];
}

export interface Page<T> { items: T[]; page: number; size: number; totalItems: number; totalPages: number }
