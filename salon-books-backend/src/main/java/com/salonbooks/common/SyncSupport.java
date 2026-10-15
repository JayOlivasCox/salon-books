package com.salonbooks.common;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class SyncSupport {

    public enum Status { APPLIED, STALE, NOOP, ERROR }

    public record Outcome<E>(UUID id, Status status, E entity) {
    }

    /**
     * Idempotent upsert with last-write-wins on clientModifiedAt.
     * STALE means the server already has a newer edit; the returned entity is the server copy.
     */
    @Transactional
    public <E extends SyncableEntity> Outcome<E> upsert(SyncableRepository<E> repo, UUID businessId,
                                                        SyncDto<E> dto, Supplier<E> factory) {
        if (dto.id() == null) throw ApiException.badRequest("id is required (client-generated UUID)");
        Instant now = Instant.now();
        Instant modified = dto.clientModifiedAt() != null ? dto.clientModifiedAt() : now;
        boolean delete = Boolean.TRUE.equals(dto.deleted());

        E entity = repo.findById(dto.id()).orElse(null);
        if (entity != null) {
            if (!entity.getBusinessId().equals(businessId)) {
                throw new ApiException(HttpStatus.CONFLICT, "id already belongs to another business");
            }
            if (entity.getClientModifiedAt().isAfter(modified)) {
                return new Outcome<>(entity.getId(), Status.STALE, entity);
            }
        } else {
            if (delete) return new Outcome<>(dto.id(), Status.NOOP, null);
            entity = factory.get();
            entity.setId(dto.id());
            entity.setBusinessId(businessId);
            entity.setCreatedAt(now);
        }

        if (delete) {
            if (entity.getDeletedAt() == null) entity.setDeletedAt(now);
        } else {
            dto.applyTo(entity);
            entity.setDeletedAt(null);
        }
        entity.setClientModifiedAt(modified);
        entity.setUpdatedAt(now);
        return new Outcome<>(entity.getId(), Status.APPLIED, repo.saveAndFlush(entity));
    }

    @Transactional
    public <E extends SyncableEntity> void softDelete(SyncableRepository<E> repo, UUID businessId, UUID id) {
        E entity = repo.findById(id)
            .filter(e -> e.getBusinessId().equals(businessId))
            .orElseThrow(() -> ApiException.notFound("Record"));
        Instant now = Instant.now();
        if (entity.getDeletedAt() == null) entity.setDeletedAt(now);
        entity.setClientModifiedAt(now);
        entity.setUpdatedAt(now);
        repo.save(entity);
    }
}
