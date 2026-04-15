package com.meydan.meydan.service;

import com.meydan.meydan.models.entities.OrganizationCreationApplication;
import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.models.enums.ApplicationStatus;
import com.meydan.meydan.models.enums.Role;
import com.meydan.meydan.repository.OrganizationCreationApplicationRepository;
import com.meydan.meydan.repository.UserRepository;
import com.meydan.meydan.request.OrganizationApplyRequest;
import com.meydan.meydan.request.AdminReviewRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizationCreationApplicationService {

    private final OrganizationCreationApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public OrganizationCreationApplicationService(
            OrganizationCreationApplicationRepository applicationRepository,
            UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    public String applyForOrganization(Long userId, OrganizationApplyRequest request) {
        if (!request.getRulesAccepted() || !request.getNoVictimAccepted()) {
            throw new RuntimeException("Platform kurallarını ve şartları kabul etmeniz gerekmektedir.");
        }

        if (applicationRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING)) {
            throw new RuntimeException("Halihazırda bekleyen bir başvurunuz bulunmaktadır.");
        }

        OrganizationCreationApplication application = new OrganizationCreationApplication();
        application.setUserId(userId);
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

        application.setStatus(ApplicationStatus.APPROVED);
        application.setAdminNotes(request.getAdminNotes());
        applicationRepository.save(application);

        User user = userRepository.findById(application.getUserId())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));
        
        // Rolü ORGANIZER yapıyoruz.
        user.setRole(Role.ORGANIZER);
        userRepository.save(user);

        return "Başvuru onaylandı. Kullanıcı artık bir organizatör.";
    }

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