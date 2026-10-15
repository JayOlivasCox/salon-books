package com.salonbooks.expense;

import com.salonbooks.common.SyncableRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends SyncableRepository<Expense>, JpaSpecificationExecutor<Expense> {
    @Query("""
        select distinct e.vendor from Expense e
         where e.businessId = :bid and e.deletedAt is null and e.vendor is not null
         order by e.vendor
        """)
    List<String> vendors(@Param("bid") UUID businessId);
}
