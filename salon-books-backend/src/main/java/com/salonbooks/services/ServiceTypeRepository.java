package com.salonbooks.services;

import com.salonbooks.common.SyncableRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceTypeRepository extends SyncableRepository<ServiceType> {
    List<ServiceType> findByBusinessIdAndDeletedAtIsNullOrderByNameAsc(UUID businessId);
}
