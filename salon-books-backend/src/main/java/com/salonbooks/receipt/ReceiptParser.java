package com.salonbooks.receipt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns raw OCR text (from ML Kit on the phone) into a draft expense:
 * vendor, date, total, tax, and line items. Heuristic by design: the app shows
 * the result as a pre-filled form the stylist confirms or edits.
 */
public final class ReceiptParser {
    private ReceiptParser() {
    }

    public record Item(String description, BigDecimal amount) {
    }

    public record Result(String vendor, LocalDate date, BigDecimal total, BigDecimal subtotal,
                         BigDecimal tax, List<Item> items) {
    }

    // Amount at end of line, optionally followed by a tax flag like "T", "N", "TX".
    private static final Pattern TRAILING_AMOUNT = Pattern.compile(
        "(?<![\\d.,])(-?)\\$?\\s?(\\d{1,3}(?:,\\d{3})+|\\d+)([.,])(\\d{2})(?:\\s*[A-Za-z]{1,2})?\\s*$");
    private static final Pattern DATE_US = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})\\b");
    private static final Pattern DATE_ISO = Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b");
    private static final Pattern DATE_NAMED = Pattern.compile(
        "\\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\.?\\s+(\\d{1,2}),?\\s+(\\d{4})\\b",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern HAS_LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern SKIP_LABEL = Pattern.compile(
        "(change|cash|visa|master|amex|discover|debit|credit|card|tender|balance|paid|payment|"
            + "auth|approval|savings|you saved|points|rewards|account|acct)", Pattern.CASE_INSENSITIVE);
    private static final String[] MONTHS = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};

    public static Result parse(String text) {
        if (text == null) text = "";
        List<String> lines = text.lines().map(String::trim).filter(l -> !l.isEmpty()).toList();

        String vendor = null;
        LocalDate date = null;
        BigDecimal total = null, subtotal = null, tax = null, maxAmount = null;
        List<Item> items = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (date == null) date = findDate(line);

            Matcher m = TRAILING_AMOUNT.matcher(line);
            if (!m.find()) {
                if (vendor == null && i < 5 && HAS_LETTER.matcher(line).find() && !looksLikeHeaderNoise(line)) {
                    vendor = line;
                }
                continue;
            }
            BigDecimal amount = toAmount(m);
            String label = line.substring(0, m.start()).trim();
            String lower = label.toLowerCase(Locale.ROOT);
            if (maxAmount == null || amount.compareTo(maxAmount) > 0) maxAmount = amount;

            if (lower.contains("subtotal") || lower.contains("sub total") || lower.contains("sub-total")) {
                subtotal = amount;
            } else if (lower.contains("total")) {
                if (!lower.contains("item") && !lower.contains("saving")) total = amount;
            } else if (lower.matches(".*\\btax\\b.*") || lower.startsWith("tax")) {
                tax = tax == null ? amount : tax.add(amount);
            } else if (HAS_LETTER.matcher(label).find() && !SKIP_LABEL.matcher(lower).find()) {
                items.add(new Item(label, amount));
            }
        }

        if (total == null) {
            total = (subtotal != null && tax != null) ? subtotal.add(tax) : maxAmount;
        }
        return new Result(vendor, date, total, subtotal, tax, items);
    }

    private static boolean looksLikeHeaderNoise(String line) {
        String l = line.toLowerCase(Locale.ROOT);
        return l.startsWith("welcome") || l.contains("receipt") || l.contains("thank you")
            || l.startsWith("tel") || l.startsWith("phone") || l.matches("[\\d\\s()\\-#.]+");
    }

    private static BigDecimal toAmount(Matcher m) {
        String whole = m.group(2).replace(",", "");
        BigDecimal v = new BigDecimal(whole + "." + m.group(4));
        return "-".equals(m.group(1)) ? v.negate() : v;
    }

    static LocalDate findDate(String line) {
        try {
            Matcher iso = DATE_ISO.matcher(line);
            if (iso.find()) {
                return LocalDate.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3)));
            }
            Matcher us = DATE_US.matcher(line);
            if (us.find()) {
                int year = Integer.parseInt(us.group(3));
                if (year < 100) year += 2000;
                return LocalDate.of(year, Integer.parseInt(us.group(1)), Integer.parseInt(us.group(2)));
            }
            Matcher named = DATE_NAMED.matcher(line);
            if (named.find()) {
                String mon = named.group(1).toLowerCase(Locale.ROOT);
                for (int i = 0; i < MONTHS.length; i++) {
                    if (MONTHS[i].equals(mon)) {
                        return LocalDate.of(Integer.parseInt(named.group(3)), i + 1, Integer.parseInt(named.group(2)));
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // e.g. 13/45/2026; keep scanning other lines
        }
        return null;
    }
}
