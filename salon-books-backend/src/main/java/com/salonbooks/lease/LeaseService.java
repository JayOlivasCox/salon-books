package com.salonbooks.lease;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaseService {
    private final LeaseRepository leases;

    public List<Lease> overlapping(UUID businessId, LocalDate from, LocalDate to) {
        return leases.findByBusinessIdAndDeletedAtIsNullAndStartDateLessThanEqualAndEndDateGreaterThanEqual(businessId, to, from);
    }

    public BigDecimal rentForRange(UUID businessId, LocalDate from, LocalDate to) {
        return rentFor(overlapping(businessId, from, to), from, to);
    }

    public static BigDecimal rentFor(List<Lease> list, LocalDate from, LocalDate to) {
        return RentCalculator.rentFor(list.stream()
            .map(l -> new RentCalculator.Term(l.getAmount(), l.getFrequency() == RentFrequency.MONTHLY,
                l.getStartDate(), l.getEndDate()))
            .toList(), from, to);
    }

    /** Leases ending within the next N days, so the app can prompt "renew or update your rent?". */
    public List<Lease> expiring(UUID businessId, LocalDate today, int withinDays) {
        return leases.findByBusinessIdAndDeletedAtIsNullOrderByStartDateDesc(businessId).stream()
            .filter(l -> !l.getEndDate().isBefore(today) && !l.getEndDate().isAfter(today.plusDays(withinDays)))
            .toList();
    }
}
