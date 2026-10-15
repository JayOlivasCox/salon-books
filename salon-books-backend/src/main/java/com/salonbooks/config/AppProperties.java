package com.salonbooks.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(int inviteTtlHours, String deepLinkScheme, int maxSyncBatch) {
}
