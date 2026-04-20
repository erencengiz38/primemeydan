package com.meydan.meydan.service;

import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.models.enums.ApplicationStatus;
import com.meydan.meydan.models.enums.OrganizationRole;
import com.meydan.meydan.models.enums.Role;
import com.meydan.meydan.repository.*;
import com.meydan.meydan.request.OrganizationApplyRequest;
import com.meydan.meydan.request.AdminReviewRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganizationCreationApplicationService {

    private final OrganizationCreationApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationQuotaRepository organizationQuotaRepository;

    public OrganizationCreationApplicationService(
            OrganizationCreationApplicationRepository applicationRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            OrganizationQuotaRepository organizationQuotaRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.organizationQuotaRepository = organizationQuotaRepository;
    }

    @Transactional
    public String applyForOrganization(Long userId, OrganizationApplyRequest request) {
        if (!request.getRulesAccepted() || !request.getNoVictimAccepted()) {
            throw new RuntimeException("Platform kurallarını ve şartları kabul etmeniz gerekmektedir.");
        }

        if (applicationRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING)) {
            throw new RuntimeException("Halihazırda bekleyen bir başvurunuz bulunmaktadır.");
        }

        if (request.getCategoryId() == null) {
            throw new RuntimeException("Organizasyon kategorisi (categoryId) belirtilmelidir.");
        }

        categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Hata: Sistemde böyle bir kategori bulunamadı! (ID: " + request.getCategoryId() + ")"));

        OrganizationCreationApplication application = new OrganizationCreationApplication();
        application.setUserId(userId);
        application.setCategoryId(request.getCategoryId());
        application.setOrganizationName(request.getOrganizationName());
        application.setDescription(request.getDescription());
        application.setLogoUrl(request.getLogoUrl());
        application.setHasPreviousExperience(request.getHasPreviousExperience());
        application.setPreviousExperienceDetails(request.getPreviousExperienceDetails());
        application.setManagementPlan(request.getManagementPlan());
        application.setPlannedGames(request.getPlannedGames());
        application.setReachPlan(request.getReachPlan());
        application.setDiscordLink(request.getDiscordLink());
        application.setSocialMediaLinks(request.getSocialMediaLinks());
        application.setHasPrizes(request.getHasPrizes());
        application.setAveragePrizeAmount(request.getAveragePrizeAmount());
        application.setReasonForApplying(request.getReasonForApplying());
        application.setRulesAccepted(request.getRulesAccepted());
        application.setNoVictimAccepted(request.getNoVictimAccepted());
        application.setStatus(ApplicationStatus.PENDING);
        application.setAppliedAt(LocalDateTime.now()); // Güvenlik için manuel atama eklendi

        applicationRepository.save(application);
        return "Başvurunuz başarıyla alınmıştır. Admin onayından sonra bilgilendirileceksiniz.";
    }

    public List<OrganizationCreationApplication> getPendingApplications() {
        return applicationRepository.findByStatus(ApplicationStatus.PENDING);
    }
    
    public List<OrganizationCreationApplication> getMyApplications(Long userId) {
        return applicationRepository.findByUserId(userId);
    }

    @Transactional
    public String approveApplication(Long applicationId, AdminReviewRequest request) {
        OrganizationCreationApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Başvuru bulunamadı."));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Bu başvuru zaten değerlendirilmiş.");
        }

        // 1. Kullanıcının bu kategoride zaten organizasyonu var mı?
        if (membershipRepository.existsByUserIdAndRoleAndOrganization_CategoryId(application.getUserId(), OrganizationRole.OWNER, application.getCategoryId())) {
            throw new RuntimeException("Bu kullanıcının belirtilen kategoride zaten bir organizasyonu var!");
        }

        // 2. Başvuruyu onaylandı olarak işaretle
        application.setStatus(ApplicationStatus.APPROVED);
        application.setAdminNotes(request.getAdminNotes());
        applicationRepository.save(application);

        User user = userRepository.findById(application.getUserId())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));
        
        // 3. Kullanıcı Rolünü ORGANIZER yap
        user.setRole(Role.ORGANIZER);
        userRepository.save(user);

        // 4. Organizasyonu (Asıl Tabloya) Oluştur
        Organization organization = new Organization();
        organization.setCategoryId(application.getCategoryId());
        organization.setName(application.getOrganizationName());
        organization.setDescription(application.getDescription());
        organization.setLogoUrl(application.getLogoUrl());
        organization.setCreatedAt(LocalDateTime.now());
        
        Organization savedOrganization = organizationRepository.save(organization);

        // 5. Başvuru sahibini OWNER (Kurucu) olarak ekle
        OrganizationMembership membership = new OrganizationMembership();
        membership.setId(new OrganizationMembershipId(savedOrganization.getId(), user.getId()));
        membership.setOrganization(savedOrganization);
        membership.setUser(user);
        membership.setRole(OrganizationRole.OWNER); 
        membershipRepository.save(membership);

        // 6. Haftalık Kota Sistemini Başlat
        OrganizationQuota quota = new OrganizationQuota();
        quota.setOrganizationId(savedOrganization.getId());
        quota.setWeeklyLimit(BigDecimal.valueOf(5000)); // Varsayılan: 5000 MC
        quota.setCurrentSpent(BigDecimal.ZERO);
        quota.setLastResetDate(LocalDateTime.now());
        organizationQuotaRepository.save(quota);

        return "Başvuru onaylandı. Organizasyon ve kotaları başarıyla oluşturuldu.";
    }

    @Transactional
    public String rejectApplication(Long applicationId, AdminReviewRequest request) {
        OrganizationCreationApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Başvuru bulunamadı."));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Bu başvuru zaten değerlendirilmiş.");
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setAdminNotes(request.getAdminNotes());
        applicationRepository.save(application);

        return "Başvuru reddedildi.";
    }
}