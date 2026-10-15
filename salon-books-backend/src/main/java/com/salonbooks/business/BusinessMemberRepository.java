package com.salonbooks.business;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessMemberRepository extends JpaRepository<BusinessMember, UUID> {
    Optional<BusinessMember> findByBusinessIdAndUserId(UUID businessId, UUID userId);

    List<BusinessMember> findByUserId(UUID userId);

    List<BusinessMember> findByBusinessIdOrderByJoinedAt(UUID businessId);
}
