package com.salonbooks.receipt;

import com.salonbooks.common.ApiException;
import com.salonbooks.config.SupabaseProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;

/**
 * Talks to Supabase Storage with the server-side secret key. The phone never gets that key:
 * it asks us for a one-time signed upload URL, PUTs the photo there, then saves the path on the expense.
 */
@Service
public class StorageService {
    private static final int DOWNLOAD_TTL_SECONDS = 600;

    private final RestClient http;
    private final SupabaseProperties props;

    public record SignedUpload(String uploadUrl, String path) {
    }

    public record SignedUrl(String url, Instant expiresAt) {
    }

    public StorageService(SupabaseProperties props) {
        this.props = props;
        this.http = RestClient.builder()
            .baseUrl(props.baseUrl() + "/storage/v1")
            .defaultHeaders(h -> {
                h.set("apikey", props.secretKey());
                // Legacy service_role keys are JWTs and also go in Authorization.
                if (props.secretKey() != null && props.secretKey().startsWith("eyJ")) h.setBearerAuth(props.secretKey());
            })
            .build();
    }

    public SignedUpload signedUpload(String path) {
        Map<?, ?> res = call(() -> http.post()
            .uri("/object/upload/sign/" + props.receiptsBucket() + "/" + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of())
            .retrieve()
            .body(Map.class));
        Object url = res == null ? null : res.get("url");
        if (url == null) throw new ApiException(HttpStatus.BAD_GATEWAY, "Storage did not return an upload URL");
        return new SignedUpload(props.baseUrl() + "/storage/v1" + url, path);
    }

    public SignedUrl signedDownload(String path) {
        Map<?, ?> res = call(() -> http.post()
            .uri("/object/sign/" + props.receiptsBucket() + "/" + path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("expiresIn", DOWNLOAD_TTL_SECONDS))
            .retrieve()
            .body(Map.class));
        Object url = res == null ? null : res.get("signedURL");
        if (url == null) throw new ApiException(HttpStatus.BAD_GATEWAY, "Storage did not return a signed URL");
        return new SignedUrl(props.baseUrl() + "/storage/v1" + url, Instant.now().plusSeconds(DOWNLOAD_TTL_SECONDS));
    }

    private static Map<?, ?> call(java.util.function.Supplier<Map<?, ?>> s) {
        try {
            return s.get();
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Storage request failed");
        }
    }
}
