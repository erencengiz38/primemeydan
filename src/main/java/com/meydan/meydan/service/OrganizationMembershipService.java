package com.meydan.meydan.service;

import com.meydan.meydan.models.entities.OrganizationMembership;
import com.meydan.meydan.models.enums.OrganizationRole;
import com.meydan.meydan.repository.OrganizationMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrganizationMembershipService {

    private final OrganizationMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public void checkAdminOrOwner(Long organizationId, Long userId) {
        boolean isAuthorized = membershipRepository.findAll().stream()
                .filter(m -> m.getOrganization().getId().equals(organizationId) && m.getUser().getId().equals(userId))
                .findFirst()
                .map(m -> m.getRole() == OrganizationRole.ADMIN || m.getRole() == OrganizationRole.OWNER)
                .orElse(false);

        if (!isAuthorized) {
            throw new SecurityException("User is not authorized to perform this action.");
        }
    }
}
