package com.salonbooks.user;

import com.salonbooks.business.BusinessService;
import com.salonbooks.business.BusinessView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {
    private final UserService users;
    private final BusinessService businesses;

    public record MeResponse(UserProfile profile, List<BusinessView> businesses) {
    }

    /** Call right after sign-in. Creates the profile if needed and lists the user's businesses. */
    @GetMapping
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new MeResponse(users.ensure(jwt), businesses.listMine(jwt));
    }
}
