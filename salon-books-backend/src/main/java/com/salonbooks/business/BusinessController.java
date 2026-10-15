package com.salonbooks.business;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BusinessController {
    private final BusinessService businesses;
    private final InviteService invites;

    public record AcceptInviteRequest(String token) {
    }

    @PostMapping("/businesses")
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessView create(@RequestBody BusinessService.CreateBusinessRequest req, @AuthenticationPrincipal Jwt jwt) {
        return businesses.create(jwt, req);
    }

    @GetMapping("/businesses")
    public List<BusinessView> mine(@AuthenticationPrincipal Jwt jwt) {
        return businesses.listMine(jwt);
    }

    @GetMapping("/businesses/{businessId}")
    public BusinessView get(@PathVariable("businessId") UUID businessId, @AuthenticationPrincipal Jwt jwt) {
        return businesses.get(businessId, jwt);
    }

    @PatchMapping("/businesses/{businessId}/settings")
    public BusinessView settings(@PathVariable("businessId") UUID businessId,
                                 @RequestBody BusinessService.UpdateSettingsRequest req,
                                 @AuthenticationPrincipal Jwt jwt) {
        return businesses.updateSettings(businessId, jwt, req);
    }

    @GetMapping("/businesses/{businessId}/members")
    public List<BusinessService.MemberView> members(@PathVariable("businessId") UUID businessId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return businesses.members(businessId, jwt);
    }

    @DeleteMapping("/businesses/{businessId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable("businessId") UUID businessId, @PathVariable("userId") UUID userId,
                             @AuthenticationPrincipal Jwt jwt) {
        businesses.removeMember(businessId, userId, jwt);
    }

    /** Returns the token and deep link. The app renders the deep link as a QR code. */
    @PostMapping("/businesses/{businessId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteService.InviteResponse invite(@PathVariable("businessId") UUID businessId,
                                               @AuthenticationPrincipal Jwt jwt) {
        return invites.create(businessId, jwt);
    }

    @PostMapping("/invites/accept")
    public BusinessView accept(@RequestBody AcceptInviteRequest req, @AuthenticationPrincipal Jwt jwt) {
        return invites.accept(req.token(), jwt);
    }
}
