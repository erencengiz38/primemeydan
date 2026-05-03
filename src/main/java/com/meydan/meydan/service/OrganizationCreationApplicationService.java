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
    private final CloudinaryService cloudinaryService;

    public OrganizationCreationApplicationService(
            OrganizationCreationApplicationRepository applicationRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            OrganizationQuotaRepository organizationQuotaRepository,
            CloudinaryService cloudinaryService) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.organizationQuotaRepository = organizationQuotaRepository;
        this.cloudinaryService = cloudinaryService;
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
        application.setAppliedAt(LocalDateTime.now());

        application = applicationRepository.save(application);

        if (request.getLogo() != null && !request.getLogo().isEmpty()) {
            try {
                MediaAsset asset = cloudinaryService.uploadAndSaveImage(
                        request.getLogo(),
                        "ORGANIZATION_LOGO",
                        application.getId().toString()
                );
                application.setLogoUrl(asset.getImageUrl());
                applicationRepository.save(application);
            } catch (Exception e) {
                throw new RuntimeException("Logo yükleme işlemi başarısız oldu.");
            }
        }

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

        if (membershipRepository.existsByUserIdAndRoleAndOrganization_CategoryId(application.getUserId(), OrganizationRole.OWNER, application.getCategoryId())) {
            throw new RuntimeException("Bu kullanıcının belirtilen kategoride zaten bir organizasyonu var!");
        }

        application.setStatus(ApplicationStatus.APPROVED);
        application.setAdminNotes(request.getAdminNotes());
        applicationRepository.save(application);

        User user = userRepository.findById(application.getUserId())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        user.setRole(Role.ORGANIZER);
        userRepository.save(user);

        Organization organization = new Organization();
        organization.setCategoryId(application.getCategoryId());
        organization.setName(application.getOrganizationName());
        organization.setDescription(application.getDescription());
        organization.setLogoUrl(application.getLogoUrl());
        organization.setCreatedAt(LocalDateTime.now());

        Organization savedOrganization = organizationRepository.save(organization);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setId(new OrganizationMembershipId(savedOrganization.getId(), user.getId()));
        membership.setOrganization(savedOrganization);
        membership.setUser(user);
        membership.setRole(OrganizationRole.OWNER);
        membershipRepository.save(membership);

        OrganizationQuota quota = new OrganizationQuota();
        quota.setOrganizationId(savedOrganization.getId());
        quota.setWeeklyLimit(BigDecimal.valueOf(5000));
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