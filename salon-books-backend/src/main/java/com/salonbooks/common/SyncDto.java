package com.salonbooks.common;

import java.time.Instant;
import java.util.UUID;

/** Request body for creating/updating/deleting a syncable record. */
public interface SyncDto<E extends SyncableEntity> {
    UUID id();

    Instant clientModifiedAt();

    Boolean deleted();

    /** Copy fields onto the entity and validate. Not called for deletes. */
    void applyTo(E entity);
}
