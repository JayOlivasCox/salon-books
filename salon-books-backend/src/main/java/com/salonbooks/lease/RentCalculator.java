package com.salonbooks.lease;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Pure rent math. Monthly rent is prorated by days within each calendar month,
 * so a full month always equals exactly the monthly amount. Weekly rent is amount/7 per day.
 */
public final class RentCalculator {
    private RentCalculator() {
    }

    public record Term(BigDecimal amount, boolean monthly, LocalDate start, LocalDate end) {
    }

    public static BigDecimal rentFor(List<Term> terms, LocalDate from, LocalDate to) {
        BigDecimal total = BigDecimal.ZERO;
        for (Term t : terms) {
            LocalDate s = t.start().isAfter(from) ? t.start() : from;
            LocalDate e = t.end().isBefore(to) ? t.end() : to;
            if (s.isAfter(e)) continue;
            if (t.monthly()) {
                for (YearMonth ym = YearMonth.from(s); !ym.isAfter(YearMonth.from(e)); ym = ym.plusMonths(1)) {
                    LocalDate ms = ym.atDay(1).isBefore(s) ? s : ym.atDay(1);
                    LocalDate me = ym.atEndOfMonth().isAfter(e) ? e : ym.atEndOfMonth();
                    long days = ChronoUnit.DAYS.between(ms, me) + 1;
                    total = total.add(t.amount().multiply(BigDecimal.valueOf(days))
                        .divide(BigDecimal.valueOf(ym.lengthOfMonth()), 6, RoundingMode.HALF_UP));
                }
            } else {
                long days = ChronoUnit.DAYS.between(s, e) + 1;
                total = total.add(t.amount().multiply(BigDecimal.valueOf(days))
                    .divide(BigDecimal.valueOf(7), 6, RoundingMode.HALF_UP));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
