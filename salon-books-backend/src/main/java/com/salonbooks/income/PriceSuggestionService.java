package com.salonbooks.income;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * "Learned" prices. Nothing here sets a price: it returns a suggestion the app can
 * offer as an autofill chip, and the stylist decides whether to use it.
 */
@Service
@RequiredArgsConstructor
public class PriceSuggestionService {
    private static final int TYPICAL_LOOKBACK_DAYS = 180;

    private final NamedParameterJdbcTemplate jdbc;

    public enum Source { CLIENT_LAST, SERVICE_MEDIAN, NONE }

    public record ServiceHistory(UUID serviceTypeId, String serviceName, BigDecimal lastAmount,
                                 BigDecimal lastTip, LocalDate lastDate, long timesDone) {
    }

    public record PriceSuggestion(UUID clientId, UUID serviceTypeId, BigDecimal suggestedAmount,
                                  BigDecimal suggestedTip, Source source, BigDecimal typicalAmount,
                                  long typicalSampleSize, List<ServiceHistory> clientHistory) {
    }

    public PriceSuggestion suggest(UUID businessId, UUID clientId, UUID serviceTypeId) {
        List<ServiceHistory> history = clientId == null ? List.of() : clientHistory(businessId, clientId);

        UUID service = serviceTypeId;
        if (service == null) {
            // No service picked yet: assume the client's most frequent one.
            service = history.stream()
                .filter(h -> h.serviceTypeId() != null)
                .max(Comparator.comparingLong(ServiceHistory::timesDone))
                .map(ServiceHistory::serviceTypeId)
                .orElse(null);
        }

        BigDecimal typical = null;
        long sample = 0;
        if (service != null) {
            var p = new MapSqlParameterSource()
                .addValue("bid", businessId)
                .addValue("sid", service)
                .addValue("since", LocalDate.now().minusDays(TYPICAL_LOOKBACK_DAYS));
            var row = jdbc.queryForMap("""
                select percentile_cont(0.5) within group (order by service_amount) as median, count(*) as n
                  from income_entry
                 where business_id = :bid and service_type_id = :sid and deleted_at is null
                   and service_date >= :since
                """, p);
            sample = ((Number) row.get("n")).longValue();
            if (row.get("median") != null) {
                typical = BigDecimal.valueOf(((Number) row.get("median")).doubleValue()).setScale(2, RoundingMode.HALF_UP);
            }
        }

        final UUID chosen = service;
        ServiceHistory match = chosen == null ? null : history.stream()
            .filter(h -> chosen.equals(h.serviceTypeId())).findFirst().orElse(null);

        if (match != null) {
            return new PriceSuggestion(clientId, chosen, match.lastAmount(), match.lastTip(),
                Source.CLIENT_LAST, typical, sample, history);
        }
        if (typical != null) {
            return new PriceSuggestion(clientId, chosen, typical, null, Source.SERVICE_MEDIAN, typical, sample, history);
        }
        return new PriceSuggestion(clientId, chosen, null, null, Source.NONE, null, 0, history);
    }

    /** Most recent visit per service for this client, plus how many times they've had it. */
    public List<ServiceHistory> clientHistory(UUID businessId, UUID clientId) {
        var p = new MapSqlParameterSource().addValue("bid", businessId).addValue("cid", clientId);
        return jdbc.query("""
            select * from (
                select distinct on (ie.service_type_id)
                       ie.service_type_id, st.name as service_name, ie.service_amount, ie.tip_amount,
                       ie.service_date, count(*) over (partition by ie.service_type_id) as times_done
                  from income_entry ie
                  left join service_type st on st.id = ie.service_type_id
                 where ie.business_id = :bid and ie.client_id = :cid and ie.deleted_at is null
                 order by ie.service_type_id, ie.service_date desc, ie.updated_at desc
            ) h
            order by h.service_date desc
            """, p, (rs, i) -> new ServiceHistory(
            rs.getObject("service_type_id", UUID.class),
            rs.getString("service_name"),
            rs.getBigDecimal("service_amount"),
            rs.getBigDecimal("tip_amount"),
            rs.getObject("service_date", LocalDate.class),
            rs.getLong("times_done")));
    }
}
