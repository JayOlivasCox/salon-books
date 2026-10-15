package com.salonbooks.appointment;

import com.salonbooks.common.ApiException;
import com.salonbooks.common.Require;
import com.salonbooks.common.SyncDto;

import java.time.Instant;
import java.util.UUID;

public record AppointmentDto(UUID id, Instant clientModifiedAt, Boolean deleted,
                             UUID stylistId, UUID clientId, String clientLabel, UUID serviceTypeId,
                             Instant startsAt, Instant endsAt, AppointmentStatus status, String notes)
    implements SyncDto<Appointment> {

    @Override
    public void applyTo(Appointment e) {
        e.setStylistId(Require.notNull(stylistId, "stylistId"));
        e.setStartsAt(Require.notNull(startsAt, "startsAt"));
        e.setEndsAt(Require.notNull(endsAt, "endsAt"));
        if (!endsAt.isAfter(startsAt)) throw ApiException.badRequest("endsAt must be after startsAt");
        e.setClientId(clientId);
        e.setClientLabel(Require.trimToNull(clientLabel));
        e.setServiceTypeId(serviceTypeId);
        e.setStatus(status == null ? AppointmentStatus.BOOKED : status);
        e.setNotes(Require.trimToNull(notes));
    }
}
