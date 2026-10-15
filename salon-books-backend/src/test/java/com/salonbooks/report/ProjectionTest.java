package com.salonbooks.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectionTest {

    @Test
    void yoyGrowthKeepsSeasonality() {
        double[] last = {1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 2000};
        double[] cur = {1100, 1100, 1100, 1100, 1100, 1100, 0, 0, 0, 0, 0, 0};
        Projection.Result r = Projection.nextYear(cur, last, 6);
        assertEquals(Projection.Method.YOY_GROWTH, r.method());
        assertEquals(0.10, r.growthRate(), 1e-9);
        assertEquals(1210, r.months()[0], 1e-6);   // 1100 * 1.1
        assertEquals(2420, r.months()[11], 1e-6);  // 2000 * 1.1 * 1.1, December stays the busy month
    }

    @Test
    void linearTrendWithoutHistory() {
        double[] cur = {100, 200, 300, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        Projection.Result r = Projection.nextYear(cur, new double[12], 3);
        assertEquals(Projection.Method.LINEAR_TREND, r.method());
        assertEquals(1300, r.months()[0], 1e-6);   // x=12 on y = 100 + 100x
    }

    @Test
    void insufficientData() {
        Projection.Result r = Projection.nextYear(new double[12], new double[12], 2);
        assertEquals(Projection.Method.INSUFFICIENT_DATA, r.method());
        assertEquals(0, r.total());
    }
}
