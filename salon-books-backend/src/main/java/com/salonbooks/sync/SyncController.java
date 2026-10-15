package com.salonbooks.sync;

import com.salonbooks.business.BusinessAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/sync")
@RequiredArgsConstructor
public class SyncController {
    private final BusinessAccess access;
    private final SyncService sync;

    /** Everything changed after `since` (omit for a full download), including tombstones (deletedAt set). */
    @GetMapping
    public SyncService.PullResponse pull(@PathVariable("businessId") UUID businessId,
                                         @RequestParam(name = "since", required = false) Instant since,
                                         @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        return sync.pull(businessId, since);
    }

    @PostMapping
    public SyncService.PushResponse push(@PathVariable("businessId") UUID businessId,
                                         @RequestBody SyncService.PushRequest req,
                                         @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        return sync.push(businessId, req);
    }
}
