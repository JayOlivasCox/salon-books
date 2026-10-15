package com.salonbooks.lease;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RentCalculatorTest {
    private static final LocalDate JAN1 = LocalDate.of(2026, 1, 1);

    @Test
    void fullMonthEqualsMonthlyAmount() {
        var t = new RentCalculator.Term(new BigDecimal("800.00"), true, JAN1, LocalDate.of(2026, 12, 31));
        assertEquals(new BigDecimal("800.00"),
            RentCalculator.rentFor(List.of(t), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)));
        assertEquals(new BigDecimal("9600.00"),
            RentCalculator.rentFor(List.of(t), JAN1, LocalDate.of(2026, 12, 31)));
    }

    @Test
    void leaseEndingMidMonthIsProrated() {
        var t = new RentCalculator.Term(new BigDecimal("900.00"), true, JAN1, LocalDate.of(2026, 6, 15));
        assertEquals(new BigDecimal("450.00"),
            RentCalculator.rentFor(List.of(t), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)));
    }

    @Test
    void weeklyRent() {
        var t = new RentCalculator.Term(new BigDecimal("200.00"), false, JAN1, LocalDate.of(2026, 12, 31));
        assertEquals(new BigDecimal("200.00"),
            RentCalculator.rentFor(List.of(t), LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8)));
    }
}
