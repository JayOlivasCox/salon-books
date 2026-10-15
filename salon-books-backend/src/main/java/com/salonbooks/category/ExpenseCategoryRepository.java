package com.salonbooks.category;

import com.salonbooks.common.SyncableRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseCategoryRepository extends SyncableRepository<ExpenseCategory> {
    List<ExpenseCategory> findByBusinessIdAndDeletedAtIsNullOrderBySortOrderAscNameAsc(UUID businessId);
}
