package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.ClanMember;
import com.meydan.meydan.models.entities.ClanMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClanMemberRepository extends JpaRepository<ClanMember, Long> {

    List<ClanMember> findByClanIdAndIsActiveTrue(Long clanId);

    List<ClanMember> findByUserIdAndIsActiveTrue(Long userId);

    Optional<ClanMember> findByClanIdAndUserIdAndIsActiveTrue(Long clanId, Long userId);

    boolean existsByUserIdAndCategoryIdAndIsActiveTrue(Long userId, Long categoryId);

    List<ClanMember> findByClanIdAndRoleInAndIsActiveTrue(Long clanId, List<ClanMemberRole> roles);

    Optional<ClanMember> findByIdAndIsActiveTrue(Long id);
}
