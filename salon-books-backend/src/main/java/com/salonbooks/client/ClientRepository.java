package com.salonbooks.client;

import com.salonbooks.common.SyncableRepository;

import java.util.List;
import java.util.UUID;

public interface ClientRepository extends SyncableRepository<Client> {
    List<Client> findByBusinessIdAndDeletedAtIsNullOrderByNameAsc(UUID businessId);

    List<Client> findByBusinessIdAndDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc(UUID businessId, String q);
}
