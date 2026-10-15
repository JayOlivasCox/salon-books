package com.salonbooks.receipt;

import com.salonbooks.business.BusinessAccess;
import com.salonbooks.common.ApiException;
import com.salonbooks.common.Require;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/receipts")
@RequiredArgsConstructor
public class ReceiptController {
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "heic", "webp");
    private static final int MAX_OCR_CHARS = 20_000;

    private final BusinessAccess access;
    private final StorageService storage;

    public record UploadUrlRequest(UUID expenseId, String fileExtension) {
    }

    public record ParseRequest(String text) {
    }

    /**
     * Step 1 of attaching a photo. The app PUTs the image bytes to uploadUrl
     * (Content-Type: image/jpeg etc.), then saves "path" as the expense's receiptPath.
     * Works offline-first: expenseId can be a UUID the app generated before syncing.
     */
    @PostMapping("/upload-url")
    public StorageService.SignedUpload uploadUrl(@PathVariable("businessId") UUID businessId,
                                                 @RequestBody UploadUrlRequest req,
                                                 @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        Require.notNull(req.expenseId(), "expenseId");
        String ext = req.fileExtension() == null ? "jpg" : req.fileExtension().toLowerCase(Locale.ROOT).replace(".", "");
        if (!EXTENSIONS.contains(ext)) throw ApiException.badRequest("Unsupported file type: " + ext);
        String path = businessId + "/" + req.expenseId() + "/" + UUID.randomUUID() + "." + ext;
        return storage.signedUpload(path);
    }

    /** Send the raw ML Kit text; get back a draft (vendor, date, total, tax, items) to pre-fill the form. */
    @PostMapping("/parse")
    public ReceiptParser.Result parse(@PathVariable("businessId") UUID businessId,
                                      @RequestBody ParseRequest req,
                                      @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        String text = req.text() == null ? "" : req.text();
        if (text.length() > MAX_OCR_CHARS) text = text.substring(0, MAX_OCR_CHARS);
        return ReceiptParser.parse(text);
    }
}
