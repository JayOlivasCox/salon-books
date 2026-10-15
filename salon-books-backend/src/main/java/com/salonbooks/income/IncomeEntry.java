package com.salonbooks.income;

import com.salonbooks.common.SyncableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** What the stylist actually made for one service: base price + tip. */
@Entity
@Table(name = "income_entry")
@Getter
@Setter
public class IncomeEntry extends SyncableEntity {
    @Column(name = "stylist_id", nullable = false)
    private UUID stylistId;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "service_type_id")
    private UUID serviceTypeId;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "service_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal serviceAmount;

    @Column(name = "tip_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal tipAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    private String notes;
}
