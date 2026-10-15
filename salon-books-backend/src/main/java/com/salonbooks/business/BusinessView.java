package com.salonbooks.business;

import java.util.UUID;

public record BusinessView(UUID id, String name, String timezone, RentDisplay rentDisplay,
                           ExpenseSort defaultExpenseSort, MemberRole role) {
    public static BusinessView of(Business b, MemberRole role) {
        return new BusinessView(b.getId(), b.getName(), b.getTimezone(), b.getRentDisplay(),
            b.getDefaultExpenseSort(), role);
    }
}
