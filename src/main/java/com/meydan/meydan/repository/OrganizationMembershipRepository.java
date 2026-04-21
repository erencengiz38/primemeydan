package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.OrganizationMembership;
import com.meydan.meydan.models.entities.OrganizationMembershipId;
import com.meydan.meydan.models.enums.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, OrganizationMembershipId> {

    // 1. Yetki Kontrolü İçin (Exists)
    @Query("SELECT COUNT(om) > 0 FROM OrganizationMembership om WHERE om.organization.id = :orgId AND om.user.id = :userId AND om.role IN :roles")
    boolean existsByOrganizationIdAndUserIdAndRoleIn(
            @Param("orgId") Long organizationId,
            @Param("userId") Long userId,
            @Param("roles") List<OrganizationRole> roles
    );

    // 2. Basit Üyelik Kontrolü
    @Query("SELECT COUNT(om) > 0 FROM OrganizationMembership om WHERE om.organization.id = :orgId AND om.user.id = :userId")
    boolean existsByOrganizationIdAndUserId(
            @Param("orgId") Long organizationId,
            @Param("userId") Long userId
    );

    // 3. Servis Katmanındaki .orElseThrow() İçin Gereken Nesne Dönüşü
    @Query("SELECT om FROM OrganizationMembership om WHERE om.organization.id = :orgId AND om.user.id = :userId")
    Optional<OrganizationMembership> findByOrganizationIdAndUserId(
            @Param("orgId") Long organizationId,
            @Param("userId") Long userId
    );

    // 4. Kullanıcının Üye Olduğu Tüm Kayıtları Getirir (Gerçek ID'leri Çekmek İçin Kritik!)
    @Query("SELECT om FROM OrganizationMembership om WHERE om.user.id = :userId")
    List<OrganizationMembership> findByUserId(@Param("userId") Long userId);

    // 5. Organizasyona Göre Üyeleri Listeleme
    @Query("SELECT om FROM OrganizationMembership om WHERE om.organization.id = :orgId")
    List<OrganizationMembership> findByOrganizationId(@Param("orgId") Long organizationId);

    // --- Diğer Boole Kontrolleri ---

    boolean existsByUserIdAndRole(Long userId, OrganizationRole role);

    // İlişkili tablo (Organization -> Category) üzerinden kontrol
    boolean existsByUserIdAndRoleAndOrganization_CategoryId(Long userId, OrganizationRole role, Long categoryId);
}