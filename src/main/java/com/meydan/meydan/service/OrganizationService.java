package com.meydan.meydan.service;

import com.meydan.meydan.request.Organization.CreateOrganizationRequestBody;
import com.meydan.meydan.dto.response.OrganizationQuotaResponseDTO;
import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.models.enums.OrganizationRole;
import com.meydan.meydan.repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final CategoryRepository categoryRepository; 
    private final OrganizationQuotaRepository organizationQuotaRepository;
    private final ModelMapper modelMapper;

public List<Organization> getMyOrganizations(Long userId) {
    // 1. Üyelik tablosundan kullanıcının tüm kayıtlarını çek
    List<OrganizationMembership> memberships = membershipRepository.findByUserId(userId);

    // 2. Kayıtların içindeki organizasyonları ayıkla ve listeye çevir
    return memberships.stream()
            .map(OrganizationMembership::getOrganization)
            .collect(Collectors.toList());
}
    @Transactional
    public Long createOrganization(CreateOrganizationRequestBody request, Long creatorId) {
        // Kategori Kontrolü
        categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Hata: Kategori bulunamadı!"));

        // Organizasyonu Kaydet
        Organization organization = modelMapper.map(request, Organization.class);
        organization.setId(null);
        Organization savedOrganization = organizationRepository.save(organization);

        // Üyeliği Kaydet (Owner olarak)
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        OrganizationMembership membership = new OrganizationMembership();
        membership.setId(new OrganizationMembershipId(savedOrganization.getId(), creator.getId()));
        membership.setOrganization(savedOrganization);
        membership.setUser(creator);
        membership.setRole(OrganizationRole.OWNER);
        membershipRepository.save(membership);

        // KOTAYI SIFIR (0) OLARAK BAŞLAT 💰
        OrganizationQuota quota = new OrganizationQuota();
        quota.setOrganizationId(savedOrganization.getId());
        quota.setWeeklyLimit(BigDecimal.valueOf(5000));
        quota.setCurrentSpent(BigDecimal.ZERO); // Bak burası önemli, 5000 yaparsan yine 0 MC kalır.
        quota.setLastResetDate(LocalDateTime.now());
        organizationQuotaRepository.save(quota);

        return savedOrganization.getId();
    }

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    public List<OrganizationMembership> getOrganizationMembers(Long organizationId) {
        return membershipRepository.findByOrganizationId(organizationId);
    }

    public OrganizationQuotaResponseDTO getOrganizationQuota(Long organizationId, Long requesterId) {
        OrganizationMembership membership = membershipRepository.findById(new OrganizationMembershipId(organizationId, requesterId))
                .orElseThrow(() -> new RuntimeException("Bu organizasyonun üyesi değilsiniz."));
                
        OrganizationQuota quota = organizationQuotaRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organizasyon kotası bulunamadı."));
                
        OrganizationQuotaResponseDTO dto = new OrganizationQuotaResponseDTO();
        dto.setOrganizationId(quota.getOrganizationId());
        dto.setWeeklyLimit(quota.getWeeklyLimit());
        dto.setCurrentSpent(quota.getCurrentSpent());
        dto.setRemainingQuota(quota.getWeeklyLimit().subtract(quota.getCurrentSpent()));
        dto.setLastResetDate(quota.getLastResetDate());
        
        return dto;
    }

    @Transactional
    public void removeMember(Long organizationId, Long userIdToRemove, Long requesterId) {
        OrganizationMembership requester = membershipRepository.findById(new OrganizationMembershipId(organizationId, requesterId))
                .orElseThrow(() -> new RuntimeException("Bu organizasyonun üyesi değilsiniz."));
                
        OrganizationMembership targetMember = membershipRepository.findById(new OrganizationMembershipId(organizationId, userIdToRemove))
                .orElseThrow(() -> new RuntimeException("Belirtilen kullanıcı bu organizasyonun üyesi değil."));

        if (requester.getRole() != OrganizationRole.OWNER) {
            throw new RuntimeException("Sadece Organizasyon Sahibi (OWNER) üye silebilir.");
        }

        if (userIdToRemove.equals(requesterId)) {
            throw new RuntimeException("Kendinizi silemezsiniz.");
        }

        membershipRepository.delete(targetMember);
        logger.info("Organizasyondan üye çıkarıldı. OrgId: {}, Kovan: {}, Kovulan: {}", organizationId, requesterId, userIdToRemove);
    }

    // --- ZAMANLANMIŞ GÖREV (CRON JOB): Kotaları Haftalık Sıfırlama ---
    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void resetAllOrganizationQuotasWeekly() {
        LocalDateTime now = LocalDateTime.now();
        int resetCount = organizationQuotaRepository.resetAllQuotas(now);
        logger.info("⏰ [KOTA SIFIRLAMA]: Toplam {} adet organizasyonun haftalık turnuva ödül kotası sıfırlandı.", resetCount);
    }
}
