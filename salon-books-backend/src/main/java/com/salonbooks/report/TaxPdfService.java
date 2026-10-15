package com.salonbooks.report;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.salonbooks.business.Business;
import com.salonbooks.lease.LeaseService;
import com.salonbooks.tax.ScheduleCLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.salonbooks.report.ReportModels.*;

/** Tax-ready summary for the stylist or their accountant, grouped by Schedule C line. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxPdfService {
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
    private static final Color SHADE = new Color(238, 238, 238);

    private final ReportService reports;
    private final LeaseService leases;

    public byte[] generate(Business b, int year) {
        LocalDate from = LocalDate.of(year, 1, 1), to = LocalDate.of(year, 12, 31);
        IncomeTotals income = reports.income(b.getId(), from, to);
        List<CategoryTotal> cats = reports.byCategory(b.getId(), from, to);
        BigDecimal rent = leases.rentForRange(b.getId(), from, to);
        List<ExpenseLine> lines = reports.expenseLines(b.getId(), from, to);

        // Schedule C line -> (category name -> amount)
        Map<ScheduleCLine, Map<String, BigDecimal>> byLine = new EnumMap<>(ScheduleCLine.class);
        for (CategoryTotal c : cats) {
            byLine.computeIfAbsent(c.scheduleCLine(), k -> new LinkedHashMap<>()).merge(c.name(), c.amount(), BigDecimal::add);
        }
        if (rent.signum() != 0) {
            byLine.computeIfAbsent(ScheduleCLine.RENT_OTHER_PROPERTY, k -> new LinkedHashMap<>())
                .merge("Booth rent (from lease)", rent, BigDecimal::add);
        }
        Map<String, BigDecimal> cogsParts = byLine.remove(ScheduleCLine.COGS);
        BigDecimal cogs = sum(cogsParts);
        BigDecimal grossIncome = income.gross().subtract(cogs);
        BigDecimal totalExpenses = byLine.values().stream().map(TaxPdfService::sum).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = grossIncome.subtract(totalExpenses);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.LETTER, 48, 48, 48, 48);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("Schedule C Summary, Tax Year " + year, TITLE));
            doc.add(new Paragraph(b.getName(), H2));
            doc.add(new Paragraph("Generated " + LocalDate.now(b.zone()).format(DateTimeFormatter.ISO_LOCAL_DATE), SMALL));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Part I: Income", H2));
            PdfPTable inc = table(new float[]{1.2f, 5f, 2f});
            row(inc, "", "Service income", income.services(), false);
            row(inc, "", "Tips", income.tips(), false);
            row(inc, "1", "Gross receipts", income.gross(), true);
            if (cogs.signum() != 0) {
                row(inc, "4", "Cost of goods sold (Part III)", cogs, false);
            }
            row(inc, "7", "Gross income", grossIncome, true);
            doc.add(inc);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Part II: Expenses", H2));
            PdfPTable exp = table(new float[]{1.2f, 5f, 2f});
            for (var entry : byLine.entrySet()) {
                ScheduleCLine l = entry.getKey();
                row(exp, l.line(), l.label(), sum(entry.getValue()), true);
                for (var part : entry.getValue().entrySet()) {
                    row(exp, "", "    " + part.getKey(), part.getValue(), false);
                }
            }
            row(exp, "28", "Total expenses", totalExpenses, true);
            row(exp, "31", "Net profit (or loss)", net, true);
            doc.add(exp);

            if (cogsParts != null && !cogsParts.isEmpty()) {
                doc.add(Chunk.NEWLINE);
                doc.add(new Paragraph("Part III: Cost of Goods Sold (purchases)", H2));
                PdfPTable c = table(new float[]{1.2f, 5f, 2f});
                for (var part : cogsParts.entrySet()) row(c, "", part.getKey(), part.getValue(), false);
                row(c, "", "Total purchases", cogs, true);
                doc.add(c);
                doc.add(new Paragraph("Beginning and ending inventory are not tracked in the app; "
                    + "your preparer may need those to finish Part III.", SMALL));
            }

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Notes", H2));
            doc.add(new Paragraph("Built from records entered in the app. Review with a tax professional before filing. "
                + "Meals (line 24b) are shown at full cost; usually only 50% is deductible. "
                + "Large equipment may need to be depreciated rather than expensed in one year.", SMALL));

            if (!lines.isEmpty()) {
                doc.newPage();
                doc.add(new Paragraph("Expense Detail", H2));
                PdfPTable d = table(new float[]{1.6f, 3f, 3f, 1f, 1.6f});
                header(d, "Date", "Vendor", "Category", "Line", "Amount");
                for (ExpenseLine x : lines) {
                    cell(d, x.date().toString(), BODY, Element.ALIGN_LEFT, null);
                    cell(d, x.vendor() == null ? "" : x.vendor(), BODY, Element.ALIGN_LEFT, null);
                    cell(d, x.categoryName(), BODY, Element.ALIGN_LEFT, null);
                    cell(d, x.scheduleCLine().line(), BODY, Element.ALIGN_LEFT, null);
                    cell(d, fmt(x.amount()), BODY, Element.ALIGN_RIGHT, null);
                }
                doc.add(d);
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to build PDF", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
        return out.toByteArray();
    }

    private static PdfPTable table(float[] widths) {
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        return t;
    }

    private static void row(PdfPTable t, String line, String label, BigDecimal amount, boolean strong) {
        Font f = strong ? BOLD : BODY;
        Color bg = strong ? SHADE : null;
        cell(t, line, f, Element.ALIGN_LEFT, bg);
        cell(t, label, f, Element.ALIGN_LEFT, bg);
        cell(t, fmt(amount), f, Element.ALIGN_RIGHT, bg);
    }

    private static void header(PdfPTable t, String... labels) {
        for (String l : labels) cell(t, l, BOLD, Element.ALIGN_LEFT, SHADE);
    }

    private static void cell(PdfPTable t, String text, Font f, int align, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(align);
        c.setPadding(4);
        c.setBorderColor(Color.LIGHT_GRAY);
        if (bg != null) c.setBackgroundColor(bg);
        t.addCell(c);
    }

    private static BigDecimal sum(Map<String, BigDecimal> m) {
        if (m == null) return BigDecimal.ZERO;
        return m.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String fmt(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(v);
    }
}
