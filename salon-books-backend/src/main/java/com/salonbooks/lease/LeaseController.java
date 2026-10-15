package com.salonbooks.lease;

import com.salonbooks.business.BusinessAccess;
import com.salonbooks.common.SyncSupport;
import com.salonbooks.common.SyncableController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/leases")
public class LeaseController extends SyncableController<Lease, LeaseDto> {
    private final LeaseRepository leases;
    private final LeaseService service;

    public LeaseController(LeaseRepository repo, SyncSupport sync, BusinessAccess access, LeaseService service) {
        super(repo, sync, access, Lease::new);
        this.leases = repo;
        this.service = service;
    }

    @GetMapping
    public List<Lease> list(@PathVariable("businessId") UUID businessId, @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        return leases.findByBusinessIdAndDeletedAtIsNullOrderByStartDateDesc(businessId);
    }

    @GetMapping("/expiring")
    public List<Lease> expiring(@PathVariable("businessId") UUID businessId,
                                @RequestParam(name = "withinDays", defaultValue = "30") int withinDays,
                                @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        LocalDate today = LocalDate.now(access.business(businessId).zone());
        return service.expiring(businessId, today, Math.max(0, withinDays));
    }
}
