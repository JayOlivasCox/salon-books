package com.salonbooks.category;

import com.salonbooks.tax.ScheduleCLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.salonbooks.tax.ScheduleCLine.*;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final ExpenseCategoryRepository repo;

    private record Seed(String name, ScheduleCLine line, String color) {
    }

    // Nine broad categories for a booth-rent stylist. Each maps to one Schedule C line for the tax PDF;
    // users can rename, recolor or remap them. Keep in sync with V3__rename_categories.sql.
    private static final List<Seed> DEFAULTS = List.of(
        new Seed("Supplies", SUPPLIES, "#8E44AD"),
        new Seed("Retail Resale", COGS, "#27AE60"),
        new Seed("Equipment", DEPRECIATION, "#2C3E50"),
        new Seed("Booth Rent", RENT_OTHER_PROPERTY, "#E67E22"),
        new Seed("Marketing", ADVERTISING, "#E84393"),
        new Seed("Fees & Subscriptions", COMMISSIONS_FEES, "#0984E3"),
        new Seed("Licenses & Insurance", TAXES_LICENSES, "#C0392B"),
        new Seed("Travel & Food", TRAVEL, "#00A8A8"),
        new Seed("Training & Other", OTHER, "#B2BEC3")
    );

    @Transactional
    public void seedDefaults(UUID businessId) {
        Instant now = Instant.now();
        List<ExpenseCategory> out = new ArrayList<>();
        for (int i = 0; i < DEFAULTS.size(); i++) {
            Seed s = DEFAULTS.get(i);
            ExpenseCategory c = new ExpenseCategory();
            c.setId(UUID.randomUUID());
            c.setBusinessId(businessId);
            c.setName(s.name());
            c.setScheduleCLine(s.line());
            c.setColor(s.color());
            c.setDefaultCategory(true);
            c.setSortOrder(i);
            c.setCreatedAt(now);
            c.setUpdatedAt(now);
            c.setClientModifiedAt(now);
            out.add(c);
        }
        repo.saveAll(out);
    }
}
