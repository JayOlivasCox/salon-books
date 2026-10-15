package com.salonbooks.appointment;

import com.salonbooks.common.SyncableRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends SyncableRepository<Appointment> {
    List<Appointment> findByBusinessIdAndDeletedAtIsNullAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
        UUID businessId, Instant from, Instant to);
}
