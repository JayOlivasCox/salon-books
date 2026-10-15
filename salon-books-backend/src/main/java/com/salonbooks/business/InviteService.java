package com.salonbooks.business;

import com.salonbooks.common.ApiException;
import com.salonbooks.common.CurrentUser;
import com.salonbooks.config.AppProperties;
import com.salonbooks.user.UserProfile;
import com.salonbooks.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * QR join flow for partners who share a business:
 * owner taps "Invite partner" -> app shows QR of deepLink -> partner scans, signs in,
 * app calls POST /api/invites/accept with the token. Tokens are single use and expire.
 */
@Service
@RequiredArgsConstructor
public class InviteService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BusinessInviteRepository invites;
    private final BusinessMemberRepository members;
    private final BusinessService businessService;
    private final BusinessAccess access;
    private final UserService users;
    private final AppProperties app;

    public record InviteResponse(String token, String deepLink, Instant expiresAt) {
    }

    @Transactional
    public InviteResponse create(UUID businessId, Jwt jwt) {
        access.requireOwner(businessId, jwt);
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        BusinessInvite inv = new BusinessInvite();
        inv.setId(UUID.randomUUID());
        inv.setBusinessId(businessId);
        inv.setTokenHash(sha256(token));
        inv.setCreatedBy(CurrentUser.id(jwt));
        inv.setCreatedAt(Instant.now());
        inv.setExpiresAt(Instant.now().plus(app.inviteTtlHours(), ChronoUnit.HOURS));
        invites.save(inv);

        String link = app.deepLinkScheme() + "://join?token=" + token;
        return new InviteResponse(token, link, inv.getExpiresAt());
    }

    @Transactional
    public BusinessView accept(String token, Jwt jwt) {
        if (token == null || token.isBlank()) throw ApiException.badRequest("token is required");
        UserProfile me = users.ensure(jwt);
        BusinessInvite inv = invites.findByTokenHash(sha256(token.trim()))
            .orElseThrow(() -> ApiException.notFound("Invite"));

        var existing = members.findByBusinessIdAndUserId(inv.getBusinessId(), me.getId());
        if (existing.isPresent()) {
            return BusinessView.of(access.business(inv.getBusinessId()), existing.get().getRole());
        }
        if (inv.getUsedAt() != null) throw new ApiException(HttpStatus.GONE, "Invite already used");
        if (inv.getExpiresAt().isBefore(Instant.now())) throw new ApiException(HttpStatus.GONE, "Invite expired");

        inv.setUsedBy(me.getId());
        inv.setUsedAt(Instant.now());
        invites.save(inv);
        businessService.addMember(inv.getBusinessId(), me.getId(), MemberRole.PARTNER);
        return BusinessView.of(access.business(inv.getBusinessId()), MemberRole.PARTNER);
    }

    static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
