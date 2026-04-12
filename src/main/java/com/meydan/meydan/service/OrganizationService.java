package com.meydan.meydan.service;

import com.meydan.meydan.request.Organization.CreateOrganizationRequestBody;
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
    private final CategoryRepository categoryRepository; // Kategori varlık kontrolü için ekledik
    private final ModelMapper modelMapper;

    @Transactional
    public Long createOrganization(CreateOrganizationRequestBody request, Long creatorId) {

        // 1. KATEGORİ GERÇEKTEN VAR MI KONTROLÜ (Kör uçuşu engelliyoruz)
        categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Hata: Sistemde böyle bir kategori bulunamadı! (ID: " + request.getCategoryId() + ")"));

        // 2. KULLANICI BU KATEGORİDE ZATEN ORGANİZASYON SAHİBİ Mİ?
        if (membershipRepository.existsByUserIdAndRoleAndOrganization_CategoryId(creatorId, OrganizationRole.OWNER, request.getCategoryId())) {
            throw new RuntimeException("Bu kategoride zaten bir organizasyonunuz var!");
        }

        // 3. MAP'LEME VE ORGANİZASYONU KAYDETME
        Organization organization = modelMapper.map(request, Organization.class);

        // KRİTİK DOKUNUŞ: ID'yi manuel olarak null yapıyoruz.
        // Böylece Hibernate "bu kesinlikle yeni bir kayıt" der ve update yapmaya çalışmaz.
        organization.setId(null);

        // Not: ModelMapper categoryId'yi kendi eşlediği için setCategory yapmıyoruz, hata almıyoruz.
        Organization savedOrganization = organizationRepository.save(organization);

        // 4. ÜYELİK İŞLEMLERİ (Kurucuyu OWNER olarak organizasyona bağlama)
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        OrganizationMembership membership = new OrganizationMembership();
        membership.setId(new OrganizationMembershipId(savedOrganization.getId(), creator.getId()));
        membership.setOrganization(savedOrganization);
        membership.setUser(creator);
        membership.setRole(OrganizationRole.OWNER); // Kurucu otomatik olarak OWNER yetkisi alır

        membershipRepository.save(membership);

        return savedOrganization.getId();
    }
}