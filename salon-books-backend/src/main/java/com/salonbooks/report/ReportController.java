package com.salonbooks.report;

import com.salonbooks.business.Business;
import com.salonbooks.business.BusinessAccess;
import com.salonbooks.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

import static com.salonbooks.report.ReportModels.*;

@RestController
@RequestMapping("/api/businesses/{businessId}/reports")
@RequiredArgsConstructor
public class ReportController {
    private final BusinessAccess access;
    private final ReportService reports;
    private final TaxPdfService pdf;

    /** Day screen: appointments (named or just "booked"), entries logged, and the day's totals. */
    @GetMapping("/daily")
    public DailyReport daily(@PathVariable("businessId") UUID businessId,
                             @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam(name = "stylistId", required = false) UUID stylistId,
                             @AuthenticationPrincipal Jwt jwt) {
        Business b = load(businessId, jwt);
        return reports.daily(b, date != null ? date : LocalDate.now(b.zone()), stylistId);
    }

    /** 7 days starting at start (the app decides whether weeks begin Sunday or Monday). */
    @GetMapping("/weekly")
    public WeeklyReport weekly(@PathVariable("businessId") UUID businessId,
                               @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                               @AuthenticationPrincipal Jwt jwt) {
        return reports.weekly(load(businessId, jwt), start);
    }

    @GetMapping("/monthly")
    public MonthlyReport monthly(@PathVariable("businessId") UUID businessId,
                                 @RequestParam("year") int year, @RequestParam("month") int month,
                                 @AuthenticationPrincipal Jwt jwt) {
        if (month < 1 || month > 12) throw ApiException.badRequest("month must be 1-12");
        return reports.monthly(load(businessId, jwt), year, month);
    }

    @GetMapping("/yearly")
    public YearlyReport yearly(@PathVariable("businessId") UUID businessId, @RequestParam("year") int year,
                               @AuthenticationPrincipal Jwt jwt) {
        return reports.yearly(load(businessId, jwt), year);
    }

    @GetMapping(value = "/tax-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> taxPdf(@PathVariable("businessId") UUID businessId, @RequestParam("year") int year,
                                         @AuthenticationPrincipal Jwt jwt) {
        Business b = load(businessId, jwt);
        byte[] bytes = pdf.generate(b, year);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("schedule-c-summary-" + year + ".pdf").build().toString())
            .body(bytes);
    }

    private Business load(UUID businessId, Jwt jwt) {
        access.requireMember(businessId, jwt);
        return access.business(businessId);
    }
}
