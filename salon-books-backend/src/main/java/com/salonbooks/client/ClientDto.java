package com.salonbooks.client;

import com.salonbooks.common.Require;
import com.salonbooks.common.SyncDto;

import java.time.Instant;
import java.util.UUID;

public record ClientDto(UUID id, Instant clientModifiedAt, Boolean deleted,
                        String name, String phone, String email, String notes) implements SyncDto<Client> {
    @Override
    public void applyTo(Client e) {
        e.setName(Require.notBlank(name, "name"));
        e.setPhone(Require.trimToNull(phone));
        e.setEmail(Require.trimToNull(email));
        e.setNotes(Require.trimToNull(notes));
    }
}
