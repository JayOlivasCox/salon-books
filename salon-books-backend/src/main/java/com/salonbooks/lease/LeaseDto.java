package com.salonbooks.lease;

import com.salonbooks.common.ApiException;
import com.salonbooks.common.Require;
import com.salonbooks.common.SyncDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * When the app asks for the rent cost it also asks how long the contract is.
 * Send either endDate or durationMonths (endDate wins if both are sent).
 */
public record LeaseDto(UUID id, Instant clientModifiedAt, Boolean deleted,
                       String label, BigDecimal amount, RentFrequency frequency,
                       LocalDate startDate, LocalDate endDate, Integer durationMonths, String notes)
    implements SyncDto<Lease> {

    @Override
    public void applyTo(Lease e) {
        BigDecimal amt = Require.notNull(amount, "amount");
        if (amt.signum() <= 0) throw ApiException.badRequest("amount must be positive");
        LocalDate start = Require.notNull(startDate, "startDate");
        LocalDate end;
        if (endDate != null) {
            end = endDate;
        } else if (durationMonths != null) {
            if (durationMonths <= 0) throw ApiException.badRequest("durationMonths must be positive");
            end = start.plusMonths(durationMonths).minusDays(1);
        } else {
            throw ApiException.badRequest("Send endDate or durationMonths");
        }
        if (end.isBefore(start)) throw ApiException.badRequest("endDate must be on or after startDate");

        e.setLabel(Require.trimToNull(label));
        e.setAmount(amt.setScale(2, RoundingMode.HALF_UP));
        e.setFrequency(Require.notNull(frequency, "frequency"));
        e.setStartDate(start);
        e.setEndDate(end);
        e.setNotes(Require.trimToNull(notes));
    }
}
