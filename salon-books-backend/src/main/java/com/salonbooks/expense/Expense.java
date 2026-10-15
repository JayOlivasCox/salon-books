package com.salonbooks.expense;

import com.salonbooks.common.SyncableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An expense. If it has items, any difference between total and the item sum
 * (usually sales tax) is attributed to the expense's own category in reports.
 */
@Entity
@Table(name = "expense")
@Getter
@Setter
public class Expense extends SyncableEntity {
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    private String vendor;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false)
    private boolean itemized;

    /** Path inside the private "receipts" bucket: {businessId}/{expenseId}/{file}. */
    @Column(name = "receipt_path")
    private String receiptPath;

    /** Raw text from on-device ML Kit OCR, kept for re-parsing and search. */
    @Column(name = "ocr_raw_text")
    private String ocrRawText;

    private String notes;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "expense_id", nullable = false, updatable = false)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 50)
    private List<ExpenseItem> items = new ArrayList<>();
}
