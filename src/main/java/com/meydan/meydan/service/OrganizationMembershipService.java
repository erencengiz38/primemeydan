package com.meydan.meydan.service;

import com.meydan.meydan.dto.OrganizationMembershipDTO;
import com.meydan.meydan.models.entities.OrganizationMembership;
import com.meydan.meydan.models.enums.OrganizationRole;
import com.meydan.meydan.repository.OrganizationMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationMembershipService {

    private final OrganizationMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public void checkAdminOrOwner(Long organizationId, Long userId) {
        OrganizationMembership membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this organization."));

        if (membership.getRole() != OrganizationRole.ADMIN && membership.getRole() != OrganizationRole.OWNER) {
            throw new SecurityException("User is not authorized to perform this action.");
        }
    }

    @Transactional(readOnly = true)
    public List<OrganizationMembershipDTO> getMembershipsByUserId(Long userId) {
        List<OrganizationMembership> memberships = membershipRepository.findByUserId(userId);
        return memberships.stream()
                .map(membership -> OrganizationMembershipDTO.builder()
                        .organizationId(membership.getOrganization().getId())
                        .organizationName(membership.getOrganization().getName())
                        .role(membership.getRole())
                        .build())
                .collect(Collectors.toList());
    }
}
