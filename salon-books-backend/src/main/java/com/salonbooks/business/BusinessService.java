package com.salonbooks.business;

import com.salonbooks.category.CategoryService;
import com.salonbooks.common.ApiException;
import com.salonbooks.common.CurrentUser;
import com.salonbooks.common.Require;
import com.salonbooks.user.UserProfile;
import com.salonbooks.user.UserProfileRepository;
import com.salonbooks.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessService {
    private final BusinessRepository businesses;
    private final BusinessMemberRepository members;
    private final UserProfileRepository profiles;
    private final UserService users;
    private final CategoryService categories;
    private final BusinessAccess access;

    public record CreateBusinessRequest(String name, String timezone) {
    }

    public record UpdateSettingsRequest(String name, String timezone, RentDisplay rentDisplay,
                                        ExpenseSort defaultExpenseSort) {
    }

    public record MemberView(UUID userId, String email, String displayName, MemberRole role, Instant joinedAt) {
    }

    @Transactional
    public BusinessView create(Jwt jwt, CreateBusinessRequest req) {
        UserProfile me = users.ensure(jwt);
        Business b = new Business();
        b.setId(UUID.randomUUID());
        b.setName(Require.notBlank(req.name(), "name"));
        b.setTimezone(validZone(req.timezone() == null ? "America/Los_Angeles" : req.timezone()));
        b.setRentDisplay(RentDisplay.MONTHLY);
        b.setDefaultExpenseSort(ExpenseSort.DATE);
        b.setCreatedBy(me.getId());
        b.setCreatedAt(Instant.now());
        businesses.save(b);

        addMember(b.getId(), me.getId(), MemberRole.OWNER);
        categories.seedDefaults(b.getId());
        return BusinessView.of(b, MemberRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<BusinessView> listMine(Jwt jwt) {
        List<BusinessMember> mine = members.findByUserId(CurrentUser.id(jwt));
        Map<UUID, Business> byId = businesses.findAllById(mine.stream().map(BusinessMember::getBusinessId).toList())
            .stream().collect(Collectors.toMap(Business::getId, Function.identity()));
        return mine.stream()
            .filter(m -> byId.containsKey(m.getBusinessId()))
            .map(m -> BusinessView.of(byId.get(m.getBusinessId()), m.getRole()))
            .toList();
    }

    @Transactional(readOnly = true)
    public BusinessView get(UUID businessId, Jwt jwt) {
        BusinessMember m = access.requireMember(businessId, jwt);
        return BusinessView.of(access.business(businessId), m.getRole());
    }

    @Transactional
    public BusinessView updateSettings(UUID businessId, Jwt jwt, UpdateSettingsRequest req) {
        BusinessMember m = access.requireMember(businessId, jwt);
        Business b = access.business(businessId);
        if (req.name() != null) b.setName(Require.notBlank(req.name(), "name"));
        if (req.timezone() != null) b.setTimezone(validZone(req.timezone()));
        if (req.rentDisplay() != null) b.setRentDisplay(req.rentDisplay());
        if (req.defaultExpenseSort() != null) b.setDefaultExpenseSort(req.defaultExpenseSort());
        return BusinessView.of(businesses.save(b), m.getRole());
    }

    @Transactional(readOnly = true)
    public List<MemberView> members(UUID businessId, Jwt jwt) {
        access.requireMember(businessId, jwt);
        List<BusinessMember> list = members.findByBusinessIdOrderByJoinedAt(businessId);
        Map<UUID, UserProfile> people = profiles.findAllById(list.stream().map(BusinessMember::getUserId).toList())
            .stream().collect(Collectors.toMap(UserProfile::getId, Function.identity()));
        return list.stream().map(m -> {
            UserProfile p = people.get(m.getUserId());
            return new MemberView(m.getUserId(), p == null ? null : p.getEmail(),
                p == null ? null : p.getDisplayName(), m.getRole(), m.getJoinedAt());
        }).toList();
    }

    /** Owner can remove a partner. A partner can remove themselves (leave). */
    @Transactional
    public void removeMember(UUID businessId, UUID userId, Jwt jwt) {
        BusinessMember me = access.requireMember(businessId, jwt);
        boolean self = me.getUserId().equals(userId);
        if (!self && me.getRole() != MemberRole.OWNER) throw ApiException.forbidden("Only the owner can remove partners");
        if (self && me.getRole() == MemberRole.OWNER) throw ApiException.badRequest("The owner cannot leave their own business");
        BusinessMember target = members.findByBusinessIdAndUserId(businessId, userId)
            .orElseThrow(() -> ApiException.notFound("Member"));
        members.delete(target);
    }

    BusinessMember addMember(UUID businessId, UUID userId, MemberRole role) {
        BusinessMember m = new BusinessMember();
        m.setId(UUID.randomUUID());
        m.setBusinessId(businessId);
        m.setUserId(userId);
        m.setRole(role);
        m.setJoinedAt(Instant.now());
        return members.save(m);
    }

    private static String validZone(String tz) {
        try {
            return ZoneId.of(tz).getId();
        } catch (Exception e) {
            throw ApiException.badRequest("Unknown timezone: " + tz);
        }
    }
}
