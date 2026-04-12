package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.OrganizationMembership;
import com.meydan.meydan.models.entities.OrganizationMembershipId;
import com.meydan.meydan.models.enums.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, OrganizationMembershipId> {

    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);
    
    boolean existsByOrganizationIdAndUserIdAndRoleIn(Long organizationId, Long userId, List<OrganizationRole> roles);

    boolean existsByUserIdAndRole(Long userId, OrganizationRole role);
    
    boolean existsByUserIdAndRoleAndOrganization_CategoryId(Long userId, OrganizationRole role, Long categoryId);
}
