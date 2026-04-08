package com.meydan.meydan.service;

import com.meydan.meydan.request.Auth.Organization.CreateOrganizationRequestBody;
import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.models.enums.OrganizationRole;
import com.meydan.meydan.repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public Long createOrganization(CreateOrganizationRequestBody request, Long creatorId) {

        if (membershipRepository.existsByUserIdAndRoleAndOrganization_CategoryId(creatorId, OrganizationRole.OWNER, request.getCategoryId())) {
            throw new RuntimeException("Bu kategoride zaten bir organizasyonunuz var!");
        }

        // 1. Map'leme işlemini yap
        Organization organization = modelMapper.map(request, Organization.class);

        // 2. KRİTİK DOKUNUŞ: ID'yi manuel olarak null yap.
        // Böylece Hibernate "bu kesinlikle yeni bir kayıt" der ve update yapmaya çalışmaz.
        organization.setId(null);

        // 3. Kaydet
        Organization savedOrganization = organizationRepository.save(organization);

        // 4. Üyelik işlemlerini yap (Aynı şekilde devam)
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        OrganizationMembership membership = new OrganizationMembership();
        membership.setId(new OrganizationMembershipId(savedOrganization.getId(), creator.getId()));
        membership.setOrganization(savedOrganization);
        membership.setUser(creator);
        membership.setRole(OrganizationRole.OWNER);

        membershipRepository.save(membership);

        return savedOrganization.getId();
    }
}