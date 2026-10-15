package com.salonbooks.client;

import com.salonbooks.business.BusinessAccess;
import com.salonbooks.common.SyncSupport;
import com.salonbooks.common.SyncableController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/clients")
public class ClientController extends SyncableController<Client, ClientDto> {
    private final ClientRepository clients;

    public ClientController(ClientRepository repo, SyncSupport sync, BusinessAccess access) {
        super(repo, sync, access, Client::new);
        this.clients = repo;
    }

    @GetMapping
    public List<Client> list(@PathVariable("businessId") UUID businessId,
                             @RequestParam(name = "q", required = false) String q,
                             @AuthenticationPrincipal Jwt jwt) {
        access.requireMember(businessId, jwt);
        if (q == null || q.isBlank()) return clients.findByBusinessIdAndDeletedAtIsNullOrderByNameAsc(businessId);
        return clients.findByBusinessIdAndDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc(businessId, q.trim());
    }
}
