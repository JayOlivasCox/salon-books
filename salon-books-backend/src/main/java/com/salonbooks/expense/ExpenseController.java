package com.salonbooks.expense;

import com.salonbooks.business.BusinessAccess;
import com.salonbooks.business.ExpenseSort;
import com.salonbooks.common.ApiException;
import com.salonbooks.common.SyncSupport;
import com.salonbooks.common.SyncableController;
import com.salonbooks.receipt.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/expenses")
public class ExpenseController extends SyncableController<Expense, ExpenseDto> {
    private final ExpenseRepository expenses;
    private final StorageService storage;

    public ExpenseController(ExpenseRepository repo, SyncSupport sync, BusinessAccess access, StorageService storage) {
        super(repo, sync, access, Expense::new);
        this.expenses = repo;
        this.storage = storage;
    }

    public record PageResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }

    /**
     * Filter by date range, category, vendor (contains), and amount range.
     * sort defaults to the business setting (DATE unless changed); dir defaults to desc.
     */
    @GetMapping
    public PageResult<Expense> list(@PathVariable("businessId") UUID businessId,
                                    @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                    @RequestParam(name = "categoryId", required = false) UUID categoryId,
                                    @RequestParam(name = "vendor", required = false) String vendor,
                                    @RequestParam(name = "minAmount", required = false) BigDecimal minAmount,
                                    @RequestParam(name = "maxAmount", required = false) BigDecimal maxAmount,
                                    @RequestParam(name = "sort", required = false) ExpenseSort sort,
                                    @RequestParam(name = "dir", defaultValue = "desc") String dir,
                                    @RequestParam(name = "page", defaultValue = "0") int page,
                                    @RequestParam(name = "size", defaultValue = "50") int size,
                                    @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        ExpenseSort s = sort != null ? sort : access.business(businessId).getDefaultExpenseSort();
        Sort.Direction d = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = switch (s) {
            case DATE -> "expenseDate";
            case CATEGORY -> "categoryId";
            case VENDOR -> "vendor";
            case AMOUNT -> "total";
        };
        Sort order = Sort.by(d, property).and(Sort.by(Sort.Direction.DESC, "expenseDate")).and(Sort.by("id"));
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), order);

        Page<Expense> result = expenses.findAll(
            ExpenseFilters.of(businessId, from, to, categoryId, vendor, minAmount, maxAmount), pr);
        return new PageResult<>(result.getContent(), result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages());
    }

    /** For the vendor filter dropdown. */
    @GetMapping("/vendors")
    public List<String> vendors(@PathVariable("businessId") UUID businessId, @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        return expenses.vendors(businessId);
    }

    /** Short-lived URL to display the receipt photo. */
    @GetMapping("/{id}/receipt-url")
    public StorageService.SignedUrl receiptUrl(@PathVariable("businessId") UUID businessId, @PathVariable("id") UUID id,
                                               @AuthenticationPrincipal Jwt jwt) {
        Expense e = get(businessId, id, jwt);
        if (e.getReceiptPath() == null) throw ApiException.notFound("Receipt");
        return storage.signedDownload(e.getReceiptPath());
    }
}
