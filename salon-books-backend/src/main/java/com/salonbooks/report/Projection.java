package com.salonbooks.report;

/**
 * Next-year projection from monthly totals. Pure math, unit tested.
 *
 * YOY_GROWTH: if last year has data in months that this year also has, growth =
 *   sum(this year) / sum(last year) over those months. Each month next year =
 *   (this year's actual month, or last year's month scaled by growth if not finished yet) * growth.
 *   This keeps seasonality (busy December, slow January).
 * LINEAR_TREND: no usable history, but 3+ finished months this year: straight-line fit, extended 12 months.
 * INSUFFICIENT_DATA: otherwise.
 */
public final class Projection {
    private Projection() {
    }

    public enum Method { YOY_GROWTH, LINEAR_TREND, INSUFFICIENT_DATA }

    public record Result(Method method, double growthRate, double[] months, double total) {
    }

    public static Result nextYear(double[] thisYear, double[] lastYear, int completedMonths) {
        int c = Math.max(0, Math.min(12, completedMonths));
        if (c == 0) return new Result(Method.INSUFFICIENT_DATA, 0, new double[12], 0);

        double thisOverlap = 0, lastOverlap = 0, thisSum = 0;
        for (int i = 0; i < c; i++) {
            thisSum += thisYear[i];
            if (thisYear[i] > 0 && lastYear[i] > 0) {
                thisOverlap += thisYear[i];
                lastOverlap += lastYear[i];
            }
        }

        if (lastOverlap > 0) {
            double g = thisOverlap / lastOverlap;
            double avgThis = thisSum / c;
            double[] out = new double[12];
            double total = 0;
            for (int i = 0; i < 12; i++) {
                double base;
                if (i < c) base = thisYear[i];
                else base = lastYear[i] > 0 ? lastYear[i] * g : avgThis;
                out[i] = Math.max(0, base * g);
                total += out[i];
            }
            return new Result(Method.YOY_GROWTH, g - 1, out, total);
        }

        if (c >= 3) {
            double n = c, sx = 0, sy = 0, sxy = 0, sxx = 0;
            for (int i = 0; i < c; i++) {
                sx += i;
                sy += thisYear[i];
                sxy += i * thisYear[i];
                sxx += (double) i * i;
            }
            double denom = n * sxx - sx * sx;
            double slope = denom == 0 ? 0 : (n * sxy - sx * sy) / denom;
            double intercept = (sy - slope * sx) / n;
            double[] out = new double[12];
            double total = 0;
            for (int i = 0; i < 12; i++) {
                out[i] = Math.max(0, intercept + slope * (12 + i));
                total += out[i];
            }
            double annualized = thisSum * 12.0 / c;
            double growth = annualized > 0 ? total / annualized - 1 : 0;
            return new Result(Method.LINEAR_TREND, growth, out, total);
        }

        return new Result(Method.INSUFFICIENT_DATA, 0, new double[12], 0);
    }
}
