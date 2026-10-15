package com.salonbooks.lease;

import com.salonbooks.common.SyncableRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeaseRepository extends SyncableRepository<Lease> {
    /** Leases that overlap [from, to]. */
    List<Lease> findByBusinessIdAndDeletedAtIsNullAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        UUID businessId, LocalDate to, LocalDate from);

    List<Lease> findByBusinessIdAndDeletedAtIsNullOrderByStartDateDesc(UUID businessId);
}
