package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.OrganizationMembership;
import com.meydan.meydan.models.entities.OrganizationMembershipId;
import com.meydan.meydan.models.enums.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, OrganizationMembershipId> {

    // Custom yazdığın metod burada durabilir, sorun yok.
    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);

    // findById(OrganizationMembershipId id) metodunu buraya YAZMIYORUZ.
    // Çünkü Spring Data JPA onu bizim için arka planda zaten hazırladı!
    boolean existsByUserIdAndRole(Long userId, OrganizationRole role);
    // Kullanıcının belirli bir organizasyonda, o organizasyonun kategorisinde OWNER olup olmadığına bakar
    boolean existsByUserIdAndRoleAndOrganization_CategoryId(Long userId, OrganizationRole role, Long categoryId);}