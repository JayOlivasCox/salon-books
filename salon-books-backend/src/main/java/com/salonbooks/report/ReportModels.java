package com.salonbooks.report;

import com.salonbooks.appointment.AppointmentStatus;
import com.salonbooks.income.IncomeEntry;
import com.salonbooks.tax.ScheduleCLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ReportModels {
    private ReportModels() {
    }

    public record IncomeTotals(BigDecimal services, BigDecimal tips, BigDecimal gross, long entries) {
    }

    public record CategoryTotal(UUID categoryId, String name, String color, ScheduleCLine scheduleCLine, BigDecimal amount) {
    }

    public record VendorTotal(String vendor, BigDecimal amount, long count) {
    }

    /** expenses excludes rent; totalExpenses = expenses + rent; net = gross - totalExpenses. */
    public record PeriodSummary(LocalDate from, LocalDate to, IncomeTotals income, BigDecimal expenses,
                                BigDecimal rent, BigDecimal totalExpenses, BigDecimal net,
                                List<CategoryTotal> byCategory, List<VendorTotal> topVendors) {
    }

    public record AppointmentView(UUID id, Instant startsAt, Instant endsAt, UUID stylistId, UUID clientId,
                                  String clientName, UUID serviceTypeId, String serviceName,
                                  AppointmentStatus status, UUID incomeEntryId) {
    }

    /** unnamedCount = booked slots with no client attached (shown as "booked" blocks). */
    public record DailyReport(LocalDate date, List<AppointmentView> appointments, int bookedCount, int unnamedCount,
                              List<IncomeEntry> entries, IncomeTotals totals) {
    }

    public record DayTotal(LocalDate date, BigDecimal services, BigDecimal tips, BigDecimal gross) {
    }

    public record WeeklyReport(LocalDate weekStart, PeriodSummary summary, boolean showRent, List<DayTotal> days) {
    }

    public record Comparison(LocalDate from, LocalDate to, BigDecimal gross, BigDecimal totalExpenses, BigDecimal net,
                             Double grossChangePct, Double netChangePct) {
    }

    public record MonthlyReport(int year, int month, PeriodSummary summary, boolean showRent,
                                Comparison previousMonth, Comparison sameMonthLastYear) {
    }

    public record MonthRow(int month, BigDecimal services, BigDecimal tips, BigDecimal gross,
                           BigDecimal expenses, BigDecimal rent, BigDecimal net) {
    }

    public record MonthStat(int month, BigDecimal gross) {
    }

    public record ProjectionView(Projection.Method method, double growthRate, List<BigDecimal> grossByMonth,
                                 List<BigDecimal> expensesByMonth, BigDecimal gross, BigDecimal expenses, BigDecimal net) {
    }

    public record YearlyReport(int year, PeriodSummary summary, List<MonthRow> months,
                               MonthStat busiestMonth, MonthStat slowestMonth, ProjectionView projectionNextYear) {
    }

    /** A single attributed expense line (an item, or the un-itemized remainder of an expense). */
    public record ExpenseLine(LocalDate date, String vendor, UUID categoryId, String categoryName,
                              ScheduleCLine scheduleCLine, BigDecimal amount) {
    }
}
