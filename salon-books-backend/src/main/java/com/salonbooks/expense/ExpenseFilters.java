package com.salonbooks.expense;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ExpenseFilters {
    private ExpenseFilters() {
    }

    static Specification<Expense> of(UUID businessId, LocalDate from, LocalDate to, UUID categoryId,
                                     String vendor, BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("businessId"), businessId));
            p.add(cb.isNull(root.get("deletedAt")));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.get("expenseDate"), to));
            if (categoryId != null) p.add(cb.equal(root.get("categoryId"), categoryId));
            if (vendor != null && !vendor.isBlank()) {
                String like = "%" + vendor.trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.like(cb.lower(root.get("vendor")), like, '\\'));
            }
            if (minAmount != null) p.add(cb.greaterThanOrEqualTo(root.get("total"), minAmount));
            if (maxAmount != null) p.add(cb.lessThanOrEqualTo(root.get("total"), maxAmount));
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
