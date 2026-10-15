package com.salonbooks.services;

import com.salonbooks.business.BusinessAccess;
import com.salonbooks.common.SyncSupport;
import com.salonbooks.common.SyncableController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/service-types")
public class ServiceTypeController extends SyncableController<ServiceType, ServiceTypeDto> {
    private final ServiceTypeRepository serviceTypes;

    public ServiceTypeController(ServiceTypeRepository repo, SyncSupport sync, BusinessAccess access) {
        super(repo, sync, access, ServiceType::new);
        this.serviceTypes = repo;
    }

    @GetMapping
    public List<ServiceType> list(@PathVariable("businessId") UUID businessId, @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        return serviceTypes.findByBusinessIdAndDeletedAtIsNullOrderByNameAsc(businessId);
    }
}
