package com.salonbooks.expense;

import com.salonbooks.common.ApiException;
import com.salonbooks.common.Require;
import com.salonbooks.common.SyncDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ExpenseDto(UUID id, Instant clientModifiedAt, Boolean deleted,
                         LocalDate expenseDate, String vendor, BigDecimal total, UUID categoryId,
                         String receiptPath, String ocrRawText, String notes, List<Item> items)
    implements SyncDto<Expense> {

    public record Item(UUID id, String description, BigDecimal amount, UUID categoryId) {
    }

    @Override
    public void applyTo(Expense e) {
        e.setExpenseDate(Require.notNull(expenseDate, "expenseDate"));
        e.setTotal(Require.nonNegative(total, "total").setScale(2, RoundingMode.HALF_UP));
        e.setVendor(Require.trimToNull(vendor));
        e.setCategoryId(categoryId);
        e.setNotes(Require.trimToNull(notes));
        e.setOcrRawText(ocrRawText);

        String path = Require.trimToNull(receiptPath);
        if (path != null && !path.startsWith(e.getBusinessId() + "/")) {
            throw ApiException.badRequest("receiptPath must come from the upload-url endpoint");
        }
        e.setReceiptPath(path);

        mergeItems(e, items == null ? List.of() : items);
        e.setItemized(!e.getItems().isEmpty());
    }

    /** Update items in place by id so Hibernate doesn't delete+reinsert rows. */
    private static void mergeItems(Expense e, List<Item> incoming) {
        Map<UUID, ExpenseItem> existing = new HashMap<>();
        for (ExpenseItem it : e.getItems()) existing.put(it.getId(), it);

        Set<UUID> keep = new HashSet<>();
        int order = 0;
        for (Item in : incoming) {
            UUID itemId = in.id() != null ? in.id() : UUID.randomUUID();
            ExpenseItem item = existing.get(itemId);
            if (item == null) {
                item = new ExpenseItem();
                item.setId(itemId);
                e.getItems().add(item);
            }
            item.setDescription(Require.notBlank(in.description(), "items.description"));
            item.setAmount(Require.notNull(in.amount(), "items.amount").setScale(2, RoundingMode.HALF_UP));
            item.setCategoryId(in.categoryId());
            item.setSortOrder(order++);
            keep.add(itemId);
        }
        e.getItems().removeIf(it -> !keep.contains(it.getId()));
    }
}
