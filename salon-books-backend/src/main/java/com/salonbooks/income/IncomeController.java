package com.salonbooks.income;

import com.salonbooks.business.BusinessAccess;
import com.salonbooks.common.ApiException;
import com.salonbooks.common.SyncSupport;
import com.salonbooks.common.SyncableController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/income")
public class IncomeController extends SyncableController<IncomeEntry, IncomeDto> {
    private final IncomeEntryRepository income;
    private final PriceSuggestionService suggestions;

    public IncomeController(IncomeEntryRepository repo, SyncSupport sync, BusinessAccess access,
                            PriceSuggestionService suggestions) {
        super(repo, sync, access, IncomeEntry::new);
        this.income = repo;
        this.suggestions = suggestions;
    }

    @GetMapping
    public List<IncomeEntry> list(@PathVariable("businessId") UUID businessId,
                                  @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(name = "stylistId", required = false) UUID stylistId,
                                  @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        if (ChronoUnit.DAYS.between(from, to) > 366) throw ApiException.badRequest("Range too large (max 1 year)");
        return income.findByBusinessIdAndDeletedAtIsNullAndServiceDateBetweenOrderByServiceDateAscCreatedAtAsc(businessId, from, to)
            .stream()
            .filter(e -> stylistId == null || stylistId.equals(e.getStylistId()))
            .toList();
    }

    /** Suggested service price/tip for the entry panel. clientId and serviceTypeId are both optional. */
    @GetMapping("/suggestion")
    public PriceSuggestionService.PriceSuggestion suggestion(@PathVariable("businessId") UUID businessId,
                                                             @RequestParam(name = "clientId", required = false) UUID clientId,
                                                             @RequestParam(name = "serviceTypeId", required = false) UUID serviceTypeId,
                                                             @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        return suggestions.suggest(businessId, clientId, serviceTypeId);
    }
}
