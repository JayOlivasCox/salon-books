package com.salonbooks.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-factor enforcement. A user who has turned on 2FA must send a token that was
 * upgraded with their authenticator code ("aal2"). A password-only token ("aal1")
 * is rejected with 403 and code "mfa_required", so a stolen password alone can't
 * reach their books. Users without 2FA are unaffected.
 *
 * Whether a user has 2FA comes from Supabase's auth.mfa_factors table, cached briefly.
 * If that check fails we refuse the request rather than let it through.
 */
@Slf4j
public class MfaFilter extends OncePerRequestFilter {
    private static final long CACHE_MS = 30_000;

    private final JdbcTemplate jdbc;
    private final Map<UUID, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(boolean hasFactor, long expiresAt) {
    }

    public MfaFilter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken token) || "aal2".equals(token.getToken().getClaimAsString("aal"))) {
            chain.doFilter(request, response);
            return;
        }

        boolean hasFactor;
        try {
            hasFactor = hasVerifiedFactor(UUID.fromString(token.getToken().getSubject()));
        } catch (DataAccessException | IllegalArgumentException e) {
            log.error("Could not check two-factor status", e);
            reject(response, 503, "service_unavailable", "Couldn't verify your sign-in right now. Please try again later.");
            return;
        }

        if (hasFactor) {
            reject(response, 403, "mfa_required", "Enter the code from your authenticator app to continue.");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean hasVerifiedFactor(UUID userId) {
        long now = System.currentTimeMillis();
        Cached c = cache.get(userId);
        if (c != null && c.expiresAt() > now) return c.hasFactor();
        Boolean found = jdbc.queryForObject(
            "select exists (select 1 from auth.mfa_factors where user_id = ? and status = 'verified')",
            Boolean.class, userId);
        boolean result = Boolean.TRUE.equals(found);
        cache.put(userId, new Cached(result, now + CACHE_MS));
        return result;
    }

    private static void reject(HttpServletResponse response, int status, String code, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write(
            "{\"type\":\"about:blank\",\"status\":" + status + ",\"code\":\"" + code + "\",\"detail\":\"" + detail + "\"}");
    }
}
