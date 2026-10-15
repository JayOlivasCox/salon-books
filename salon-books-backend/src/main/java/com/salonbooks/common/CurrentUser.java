package com.salonbooks.common;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.UUID;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public static String email(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    public static String displayName(Jwt jwt) {
        Map<String, Object> meta = jwt.getClaimAsMap("user_metadata");
        if (meta == null) return null;
        Object name = meta.getOrDefault("full_name", meta.get("name"));
        return name == null ? null : name.toString();
    }
}
