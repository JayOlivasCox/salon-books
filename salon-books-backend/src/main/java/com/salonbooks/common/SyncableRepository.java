package com.salonbooks.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@NoRepositoryBean
public interface SyncableRepository<E extends SyncableEntity> extends JpaRepository<E, UUID> {
    List<E> findByBusinessIdAndUpdatedAtAfter(UUID businessId, Instant since);

    List<E> findByBusinessIdAndDeletedAtIsNull(UUID businessId);
}
