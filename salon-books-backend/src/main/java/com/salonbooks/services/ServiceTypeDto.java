package com.salonbooks.services;

import com.salonbooks.common.ApiException;
import com.salonbooks.common.Require;
import com.salonbooks.common.SyncDto;

import java.time.Instant;
import java.util.UUID;

public record ServiceTypeDto(UUID id, Instant clientModifiedAt, Boolean deleted,
                             String name, Integer defaultDurationMinutes) implements SyncDto<ServiceType> {
    @Override
    public void applyTo(ServiceType e) {
        e.setName(Require.notBlank(name, "name"));
        if (defaultDurationMinutes != null && defaultDurationMinutes <= 0) {
            throw ApiException.badRequest("defaultDurationMinutes must be positive");
        }
        e.setDefaultDurationMinutes(defaultDurationMinutes);
    }
}
