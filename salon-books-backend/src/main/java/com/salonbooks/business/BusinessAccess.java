package com.salonbooks.business;

import com.salonbooks.common.ApiException;
import com.salonbooks.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Every /api/businesses/{id}/... endpoint goes through here first. */
@Component
@RequiredArgsConstructor
public class BusinessAccess {
    private final BusinessMemberRepository members;
    private final BusinessRepository businesses;

    /** 404 instead of 403 so outsiders cannot probe which business ids exist. */
    public BusinessMember requireMember(UUID businessId, Jwt jwt) {
        return members.findByBusinessIdAndUserId(businessId, CurrentUser.id(jwt))
            .orElseThrow(() -> ApiException.notFound("Business"));
    }

    public BusinessMember requireOwner(UUID businessId, Jwt jwt) {
        BusinessMember m = requireMember(businessId, jwt);
        if (m.getRole() != MemberRole.OWNER) throw ApiException.forbidden("Only the owner can do this");
        return m;
    }

    public Business business(UUID businessId) {
        return businesses.findById(businessId).orElseThrow(() -> ApiException.notFound("Business"));
    }
}
