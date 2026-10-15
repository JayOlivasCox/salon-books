package com.salonbooks.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** One line on an itemized receipt. Owned by its Expense; synced as part of it. */
@Entity
@Table(name = "expense_item")
@Getter
@Setter
public class ExpenseItem {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Optional override, e.g. a Target run with both supplies and retail stock. */
    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
