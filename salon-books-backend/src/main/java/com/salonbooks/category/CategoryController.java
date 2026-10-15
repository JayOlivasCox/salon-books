package com.salonbooks.category;

import com.salonbooks.business.BusinessAccess;
import com.salonbooks.common.SyncSupport;
import com.salonbooks.common.SyncableController;
import com.salonbooks.tax.ScheduleCLine;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/categories")
public class CategoryController extends SyncableController<ExpenseCategory, CategoryDto> {
    private final ExpenseCategoryRepository categories;

    public CategoryController(ExpenseCategoryRepository repo, SyncSupport sync, BusinessAccess access) {
        super(repo, sync, access, ExpenseCategory::new);
        this.categories = repo;
    }

    public record ScheduleCLineView(String code, String line, String label) {
    }

    @GetMapping
    public List<ExpenseCategory> list(@PathVariable("businessId") UUID businessId, @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        return categories.findByBusinessIdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(businessId);
    }

    /** For the "add category" picker. */
    @GetMapping("/schedule-c-lines")
    public List<ScheduleCLineView> lines(@PathVariable("businessId") UUID businessId) {
        return Arrays.stream(ScheduleCLine.values())
            .map(l -> new ScheduleCLineView(l.name(), l.line(), l.label()))
            .toList();
    }
}
