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

    // Starting point for a booth-rent stylist. Users can rename, recolor, remap or add their own.
    private static final List<Seed> DEFAULTS = List.of(
        new Seed("Color & Chemicals", SUPPLIES, "#8E44AD"),
        new Seed("Backbar Products", SUPPLIES, "#3498DB"),
        new Seed("Tools & Small Equipment", SUPPLIES, "#16A085"),
        new Seed("Retail Products (resale)", COGS, "#27AE60"),
        new Seed("Large Equipment", DEPRECIATION, "#2C3E50"),
        new Seed("Booth Rent", RENT_OTHER_PROPERTY, "#E67E22"),
        new Seed("Licenses & Permits", TAXES_LICENSES, "#C0392B"),
        new Seed("Insurance", INSURANCE, "#7F8C8D"),
        new Seed("Education & Classes", OTHER, "#F1C40F"),
        new Seed("Advertising & Marketing", ADVERTISING, "#E84393"),
        new Seed("Card & App Fees", COMMISSIONS_FEES, "#00B894"),
        new Seed("Booking Software", OFFICE_EXPENSE, "#0984E3"),
        new Seed("Phone & Internet", UTILITIES, "#6C5CE7"),
        new Seed("Car & Mileage", CAR_TRUCK, "#636E72"),
        new Seed("Laundry & Cleaning", OTHER, "#74B9FF"),
        new Seed("Accountant & Legal", LEGAL_PROFESSIONAL, "#A29BFE"),
        new Seed("Repairs", REPAIRS, "#D63031"),
        new Seed("Travel", TRAVEL, "#00CEC9"),
        new Seed("Meals", MEALS, "#FDCB6E"),
        new Seed("Other", OTHER, "#B2BEC3")
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
