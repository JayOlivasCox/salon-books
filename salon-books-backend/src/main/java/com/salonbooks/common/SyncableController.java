package com.salonbooks.common;

import com.salonbooks.business.BusinessAccess;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Shared GET /{id}, PUT /{id} (create or update), DELETE /{id} (tombstone)
 * for every syncable resource under /api/businesses/{businessId}/...
 */
public abstract class SyncableController<E extends SyncableEntity, D extends SyncDto<E>> {
    protected final SyncableRepository<E> repo;
    protected final SyncSupport sync;
    protected final BusinessAccess access;
    private final Supplier<E> factory;

    protected SyncableController(SyncableRepository<E> repo, SyncSupport sync, BusinessAccess access, Supplier<E> factory) {
        this.repo = repo;
        this.sync = sync;
        this.access = access;
        this.factory = factory;
    }

    @GetMapping("/{id}")
    public E get(@PathVariable("businessId") UUID businessId, @PathVariable("id") UUID id,
                 @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        return repo.findById(id)
            .filter(e -> e.getBusinessId().equals(businessId) && e.getDeletedAt() == null)
            .orElseThrow(() -> ApiException.notFound("Record"));
    }

    /** 200 with the saved record, or 409 with the newer server copy if this edit is stale. */
    @PutMapping("/{id}")
    public ResponseEntity<E> put(@PathVariable("businessId") UUID businessId, @PathVariable("id") UUID id,
                                 @Valid @RequestBody D dto, @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        if (!id.equals(dto.id())) throw ApiException.badRequest("Path id and body id must match");
        SyncSupport.Outcome<E> out = sync.upsert(repo, businessId, dto, factory);
        if (out.status() == SyncSupport.Status.STALE) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(out.entity());
        }
        return ResponseEntity.ok(out.entity());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("businessId") UUID businessId, @PathVariable("id") UUID id,
                       @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        sync.softDelete(repo, businessId, id);
    }
}
