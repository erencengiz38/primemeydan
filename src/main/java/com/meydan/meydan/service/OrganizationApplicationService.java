package com.meydan.meydan.service;

import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.models.enums.ApplicationStatus;
import com.meydan.meydan.models.enums.OrganizationRole;
import com.meydan.meydan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationApplicationService {

    private final OrganizationApplicationRepository applicationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;

    @Transactional
    public OrganizationApplication applyToOrganization(Long organizationId, Long userId, String message) {

        if (applicationRepository.existsByOrganizationIdAndUserIdAndStatus(organizationId, userId, ApplicationStatus.PENDING)) {
            throw new RuntimeException("Zaten bekleyen bir başvurunuz var.");
        }

        OrganizationMembershipId membershipId = new OrganizationMembershipId(organizationId, userId);
        if (membershipRepository.findById(membershipId).isPresent()) {
            throw new RuntimeException("Kullanıcı zaten bu organizasyonun üyesi.");
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organizasyon bulunamadı."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        OrganizationApplication application = OrganizationApplication.builder()
                .organization(org)
                .user(user)
                .message(message)
                .build();

        return applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public List<OrganizationApplication> getPendingApplications(Long organizationId, Long requesterId) {
        checkOrganizerPermission(organizationId, requesterId);
        return applicationRepository.findByOrganizationIdAndStatus(organizationId, ApplicationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<OrganizationApplication> getAllApplications(Long organizationId, Long requesterId) {
        checkOrganizerPermission(organizationId, requesterId);
        return applicationRepository.findByOrganizationId(organizationId);
    }

    @Transactional
    public void approveApplication(Long applicationId, Long approverId) {
        OrganizationApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Başvuru bulunamadı."));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Bu başvuru zaten sonuçlandırılmış.");
        }

        checkOrganizerPermission(application.getOrganization().getId(), approverId);

        application.setStatus(ApplicationStatus.APPROVED);
        applicationRepository.save(application);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setId(new OrganizationMembershipId(application.getOrganization().getId(), application.getUser().getId()));
        membership.setOrganization(application.getOrganization());
        membership.setUser(application.getUser());
        membership.setRole(OrganizationRole.MEMBER);
        membershipRepository.save(membership);
    }

    @Transactional
    public void rejectApplication(Long applicationId, Long approverId) {
        OrganizationApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Başvuru bulunamadı."));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Bu başvuru zaten sonuçlandırılmış.");
        }

        checkOrganizerPermission(application.getOrganization().getId(), approverId);

        application.setStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(application);
    }

    private void checkOrganizerPermission(Long organizationId, Long userId) {
        OrganizationMembership membership = membershipRepository.findById(new OrganizationMembershipId(organizationId, userId))
                .orElseThrow(() -> new RuntimeException("Kullanıcı bu organizasyonun üyesi değil."));

        if (membership.getRole() != OrganizationRole.OWNER && membership.getRole() != OrganizationRole.ADMIN) {
            throw new RuntimeException("Bu işlemi yapmak için yetkiniz yok.");
        }
    }

    @Transactional
    public void updateApplicationStatus(Long applicationId, Long approverId, ApplicationStatus status) {

        OrganizationApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Başvuru bulunamadı."));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Bu başvuru zaten sonuçlandırılmış.");
        }

        checkOrganizerPermission(application.getOrganization().getId(), approverId);

        if (status == ApplicationStatus.APPROVED) {
            application.setStatus(ApplicationStatus.APPROVED);

            OrganizationMembership membership = new OrganizationMembership();
            membership.setId(new OrganizationMembershipId(application.getOrganization().getId(), application.getUser().getId()));
            membership.setOrganization(application.getOrganization());
            membership.setUser(application.getUser());
            membership.setRole(OrganizationRole.MEMBER);

            membershipRepository.save(membership);

        } else if (status == ApplicationStatus.REJECTED) {
            application.setStatus(ApplicationStatus.REJECTED);
        } else {
            throw new RuntimeException("Geçersiz statü işlemi.");
        }

        applicationRepository.save(application);
    }
}
