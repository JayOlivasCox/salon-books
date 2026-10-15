package com.salonbooks.income;

import com.salonbooks.common.SyncableRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IncomeEntryRepository extends SyncableRepository<IncomeEntry> {
    List<IncomeEntry> findByBusinessIdAndDeletedAtIsNullAndServiceDateBetweenOrderByServiceDateAscCreatedAtAsc(
        UUID businessId, LocalDate from, LocalDate to);
}
