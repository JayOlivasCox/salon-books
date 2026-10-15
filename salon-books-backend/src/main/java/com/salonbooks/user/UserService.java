package com.salonbooks.user;

import com.salonbooks.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserProfileRepository profiles;

    /** Creates the profile row on first sight of a Supabase user and keeps email/name fresh. */
    @Transactional
    public UserProfile ensure(Jwt jwt) {
        UUID id = CurrentUser.id(jwt);
        String email = CurrentUser.email(jwt);
        String name = CurrentUser.displayName(jwt);
        UserProfile p = profiles.findById(id).orElse(null);
        if (p == null) {
            p = new UserProfile();
            p.setId(id);
            p.setCreatedAt(Instant.now());
        } else if (Objects.equals(p.getEmail(), email) && (name == null || name.equals(p.getDisplayName()))) {
            return p;
        }
        p.setEmail(email);
        if (name != null) p.setDisplayName(name);
        return profiles.save(p);
    }
}
