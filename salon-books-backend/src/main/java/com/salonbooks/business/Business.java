package com.salonbooks.business;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "business")
@Getter
@Setter
public class Business {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "rent_display", nullable = false)
    private RentDisplay rentDisplay;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_expense_sort", nullable = false)
    private ExpenseSort defaultExpenseSort;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ZoneId zone() {
        return ZoneId.of(timezone);
    }
}
