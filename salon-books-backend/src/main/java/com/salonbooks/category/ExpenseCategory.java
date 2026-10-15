package com.salonbooks.category;

import com.salonbooks.common.SyncableEntity;
import com.salonbooks.tax.ScheduleCLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** User-facing category (e.g. "Color & Chemicals") mapped to a Schedule C line for the tax PDF. */
@Entity
@Table(name = "expense_category")
@Getter
@Setter
public class ExpenseCategory extends SyncableEntity {
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_c_line", nullable = false)
    private ScheduleCLine scheduleCLine;

    private String color;

    @Column(name = "default_category", nullable = false)
    private boolean defaultCategory;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
