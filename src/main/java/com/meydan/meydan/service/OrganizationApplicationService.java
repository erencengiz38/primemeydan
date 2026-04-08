package com.meydan.meydan.service;

import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.models.enums.ApplicationStatus;
import com.meydan.meydan.models.enums.OrganizationRole;
import com.meydan.meydan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.meydan.meydan.models.entities.OrganizationMembership;
import com.meydan.meydan.models.entities.OrganizationMembershipId;
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

        // 1. Veritabanından başvuruyu bul, yoksa hata fırlat
        OrganizationApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Başvuru bulunamadı."));

        // 2. Başvuru hala beklemede mi (PENDING) kontrol et. Zaten onaylanmış/reddedilmişse işlem yapma
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Bu başvuru zaten sonuçlandırılmış.");
        }

        // 3. İsteği atan kişinin (approverId) bu organizasyonda OWNER veya ADMIN yetkisi var mı kontrol et
        checkOrganizerPermission(application.getOrganization().getId(), approverId);

        // 4. Gelen statüye (status) göre işlemi yap
        if (status == ApplicationStatus.APPROVED) {

            // Başvuruyu onaylandı olarak işaretle
            application.setStatus(ApplicationStatus.APPROVED);

            // Kullanıcıyı organizasyonun üyeleri tablosuna MEMBER olarak ekle
            OrganizationMembership membership = new OrganizationMembership();
            membership.setId(new OrganizationMembershipId(application.getOrganization().getId(), application.getUser().getId()));
            membership.setOrganization(application.getOrganization());
            membership.setUser(application.getUser());
            membership.setRole(OrganizationRole.MEMBER);

            membershipRepository.save(membership);

        } else if (status == ApplicationStatus.REJECTED) {

            // Sadece başvuruyu reddedildi olarak işaretle, üye yapma
            application.setStatus(ApplicationStatus.REJECTED);

        } else {
            throw new RuntimeException("Geçersiz statü işlemi.");
        }

        // 5. Başvurunun güncellenmiş statüsünü veritabanına kaydet
        applicationRepository.save(application);
    }
}