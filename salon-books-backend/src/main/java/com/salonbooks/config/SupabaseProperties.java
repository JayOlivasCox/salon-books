package com.salonbooks.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public record SupabaseProperties(String url, String secretKey, String jwtSecret, String receiptsBucket) {
    public String baseUrl() {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String issuer() {
        return baseUrl() + "/auth/v1";
    }
}
