package com.salonbooks.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Base for everything the app can create offline. The phone generates the UUID,
 * so retries are idempotent. updatedAt is server time (drives pulls);
 * clientModifiedAt is phone time (drives last-write-wins). deletedAt is a tombstone.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class SyncableEntity {
    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false, updatable = false)
    private UUID businessId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "client_modified_at", nullable = false)
    private Instant clientModifiedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
