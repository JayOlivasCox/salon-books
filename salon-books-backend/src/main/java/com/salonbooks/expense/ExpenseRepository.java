package com.salonbooks.expense;

import com.salonbooks.common.SyncableRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends SyncableRepository<Expense>, JpaSpecificationExecutor<Expense> {
    @Query("""
        select distinct e.vendor from Expense e
         where e.businessId = :bid and e.deletedAt is null and e.vendor is not null
         order by e.vendor
        """)
    List<String> vendors(@Param("bid") UUID businessId);

    /** The category this vendor's expenses most often get (ties go to the most recent). */
    @Query(value = """
        select e.category_id from expense e
          join expense_category c on c.id = e.category_id and c.deleted_at is null
         where e.business_id = :bid and e.deleted_at is null
           and lower(trim(e.vendor)) = lower(trim(:vendor))
         group by e.category_id
         order by count(*) desc, max(e.expense_date) desc
         limit 1
        """, nativeQuery = true)
    Optional<UUID> topCategoryForVendor(@Param("bid") UUID businessId, @Param("vendor") String vendor);
}
