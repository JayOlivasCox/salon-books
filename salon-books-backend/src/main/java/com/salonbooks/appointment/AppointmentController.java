package com.salonbooks.appointment;

import com.salonbooks.business.BusinessAccess;
import com.salonbooks.common.ApiException;
import com.salonbooks.common.SyncSupport;
import com.salonbooks.common.SyncableController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/appointments")
public class AppointmentController extends SyncableController<Appointment, AppointmentDto> {
    private final AppointmentRepository appointments;

    public AppointmentController(AppointmentRepository repo, SyncSupport sync, BusinessAccess access) {
        super(repo, sync, access, Appointment::new);
        this.appointments = repo;
    }

    /** Appointments starting in [from, to). For the day view, prefer GET /reports/daily. */
    @GetMapping
    public List<Appointment> list(@PathVariable("businessId") UUID businessId,
                                  @RequestParam("from") Instant from,
                                  @RequestParam("to") Instant to,
                                  @RequestParam(name = "stylistId", required = false) UUID stylistId,
                                  @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        if (Duration.between(from, to).toDays() > 62) throw ApiException.badRequest("Range too large (max 62 days)");
        return appointments
            .findByBusinessIdAndDeletedAtIsNullAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(businessId, from, to)
            .stream()
            .filter(a -> stylistId == null || stylistId.equals(a.getStylistId()))
            .toList();
    }
}
