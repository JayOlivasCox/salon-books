package com.salonbooks.income;

import com.salonbooks.common.Require;
import com.salonbooks.common.SyncDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeDto(UUID id, Instant clientModifiedAt, Boolean deleted,
                        UUID stylistId, UUID appointmentId, UUID clientId, UUID serviceTypeId,
                        LocalDate serviceDate, BigDecimal serviceAmount, BigDecimal tipAmount,
                        PaymentMethod paymentMethod, String notes) implements SyncDto<IncomeEntry> {
    @Override
    public void applyTo(IncomeEntry e) {
        e.setStylistId(Require.notNull(stylistId, "stylistId"));
        e.setServiceDate(Require.notNull(serviceDate, "serviceDate"));
        e.setServiceAmount(Require.nonNegative(serviceAmount, "serviceAmount").setScale(2, RoundingMode.HALF_UP));
        BigDecimal tip = tipAmount == null ? BigDecimal.ZERO : Require.nonNegative(tipAmount, "tipAmount");
        e.setTipAmount(tip.setScale(2, RoundingMode.HALF_UP));
        e.setAppointmentId(appointmentId);
        e.setClientId(clientId);
        e.setServiceTypeId(serviceTypeId);
        e.setPaymentMethod(paymentMethod);
        e.setNotes(Require.trimToNull(notes));
    }
}
