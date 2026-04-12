package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.ClanMember;
import com.meydan.meydan.models.enums.ClanMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClanMemberRepository extends JpaRepository<ClanMember, Long> {

    List<ClanMember> findByClanIdAndIsActiveTrue(Long clanId);

    List<ClanMember> findByUserIdAndIsActiveTrue(Long userId);

    Optional<ClanMember> findByClanIdAndUserIdAndIsActiveTrue(Long clanId, Long userId);

    // ESKİ HALİ: Sadece üyeliğin aktifliğine bakıyordu
    boolean existsByUserIdAndCategoryIdAndIsActiveTrue(Long userId, Long categoryId);

    // YENİ HALİ: Hem üyeliğin HEM DE klanın aktifliğine bakar
    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END FROM ClanMember cm WHERE cm.userId = :userId AND cm.categoryId = :categoryId AND cm.isActive = true AND cm.clan.isActive = true")
    boolean existsActiveMemberInActiveClan(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    List<ClanMember> findByClanIdAndRoleInAndIsActiveTrue(Long clanId, List<ClanMemberRole> roles);

    Optional<ClanMember> findByIdAndIsActiveTrue(Long id);
}
