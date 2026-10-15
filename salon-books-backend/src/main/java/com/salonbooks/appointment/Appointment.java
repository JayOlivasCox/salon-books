package com.salonbooks.appointment;

import com.salonbooks.common.SyncableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A booked slot. clientId is optional: a stylist can block out a slot without naming
 * the client (shown as "booked" on the daily view), or use a free-text clientLabel.
 */
@Entity
@Table(name = "appointment")
@Getter
@Setter
public class Appointment extends SyncableEntity {
    @Column(name = "stylist_id", nullable = false)
    private UUID stylistId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "client_label")
    private String clientLabel;

    @Column(name = "service_type_id")
    private UUID serviceTypeId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    private String notes;
}
