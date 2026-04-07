package com.meydan.meydan.service;

import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.repository.CategoryRepository;
import com.meydan.meydan.repository.ClanInvitationRepository;
import com.meydan.meydan.repository.ClanMemberRepository;
import com.meydan.meydan.repository.ClanRepository;
import com.meydan.meydan.request.Auth.Clan.AddClanRequestBody;
import com.meydan.meydan.request.Auth.Clan.RespondToInvitationRequest;
import com.meydan.meydan.request.Auth.Clan.UpdateClanMemberRoleRequestBody;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClanService {

    private final ClanRepository clanRepository;
    private final ClanMemberRepository clanMemberRepository;
    private final CategoryRepository categoryRepository;
    private final ClanInvitationRepository clanInvitationRepository;
    private final ModelMapper modelMapper;

    // --- Clan Read Operations ---
    public List<Clan> getAllClans() {
        return clanRepository.findByIsActiveTrue();
    }

    public Page<Clan> getAllClansWithPagination(Pageable pageable) {
        return clanRepository.findByIsActiveTrue(pageable);
    }

    public List<Clan> getClansByCategory(Long categoryId) {
        return clanRepository.findByCategoryIdAndIsActiveTrue(categoryId);
    }

    public Page<Clan> getClansByCategoryWithPagination(Long categoryId, Pageable pageable) {
        return clanRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable);
    }

    public Clan getClanById(Long id) {
        return clanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Clan bulunamadı"));
    }

    // --- Clan Create Operation ---
    @Transactional
    public Clan createClan(AddClanRequestBody request, Long creatorUserId) {
        if (clanMemberRepository.existsByUserIdAndCategoryIdAndIsActiveTrue(creatorUserId, request.getCategoryId())) {
            throw new RuntimeException("Bu oyun kategorisinde zaten bir clan üyesisiniz.");
        }
        if (clanRepository.existsByCategoryIdAndNameAndIsActiveTrue(request.getCategoryId(), request.getName())) {
            throw new RuntimeException("Bu kategoride aynı isimde clan zaten var.");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Kategori bulunamadı."));

        Clan clan = modelMapper.map(request, Clan.class);
        clan.setId(null);
        clan.setCategory(category);
        Clan savedClan = clanRepository.save(clan);

        addMemberToClan(savedClan, creatorUserId, ClanMemberRole.OWNER);

        return savedClan;
    }

    // --- Invitation and Application System ---

    @Transactional
    public ClanInvitation inviteToClan(Long clanId, Long userIdToInvite, Long requesterUserId) {
        Clan clan = getClanById(clanId);
        checkPermission(clanId, requesterUserId, List.of(ClanMemberRole.OWNER, ClanMemberRole.MANAGER), "Davet gönderme yetkiniz yok.");

        if (clanMemberRepository.existsByUserIdAndCategoryIdAndIsActiveTrue(userIdToInvite, clan.getCategory().getId())) {
            throw new RuntimeException("Davet edilen kullanıcı zaten bu kategoride bir clan üyesi.");
        }
        if (clanInvitationRepository.findByClanIdAndUserIdAndStatus(clanId, userIdToInvite, ClanInvitationStatus.PENDING).isPresent()) {
            throw new RuntimeException("Bu kullanıcıya zaten bekleyen bir davet/başvuru var.");
        }

        ClanInvitation invitation = new ClanInvitation();
        invitation.setClan(clan);
        invitation.setUserId(userIdToInvite);
        invitation.setInviterId(requesterUserId);
        invitation.setType(ClanInvitationType.INVITATION);
        return clanInvitationRepository.save(invitation);
    }

    @Transactional
    public ClanInvitation applyToClan(Long clanId, Long applicantUserId) {
        Clan clan = getClanById(clanId);

        if (clanMemberRepository.existsByUserIdAndCategoryIdAndIsActiveTrue(applicantUserId, clan.getCategory().getId())) {
            throw new RuntimeException("Bu oyun kategorisinde zaten bir clan üyesisiniz.");
        }
        if (clanInvitationRepository.findByClanIdAndUserIdAndStatus(clanId, applicantUserId, ClanInvitationStatus.PENDING).isPresent()) {
            throw new RuntimeException("Bu clana zaten bekleyen bir başvurun/davetin var.");
        }

        ClanInvitation application = new ClanInvitation();
        application.setClan(clan);
        application.setUserId(applicantUserId);
        application.setInviterId(applicantUserId); // Applicant is the initiator
        application.setType(ClanInvitationType.APPLICATION);
        return clanInvitationRepository.save(application);
    }

    @Transactional
    public ClanInvitation respondToInvitation(Long invitationId, Long respondingUserId, boolean accept, String reason) {
        ClanInvitation invitation = clanInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Davet bulunamadı."));

        if (!invitation.getUserId().equals(respondingUserId)) {
            throw new RuntimeException("Bu davete cevap verme yetkiniz yok.");
        }
        if (invitation.getStatus() != ClanInvitationStatus.PENDING) {
            throw new RuntimeException("Bu davet zaten cevaplanmış veya iptal edilmiş.");
        }
        if (invitation.getType() != ClanInvitationType.INVITATION) {
            throw new RuntimeException("Bu bir davet değil, başvuru.");
        }

        if (accept) {
            invitation.setStatus(ClanInvitationStatus.ACCEPTED);
            addMemberToClan(invitation.getClan(), invitation.getUserId(), ClanMemberRole.MEMBER);
        } else {
            invitation.setStatus(ClanInvitationStatus.REJECTED);
            invitation.setReason(reason);
        }
        invitation.setRespondedAt(LocalDateTime.now());
        return clanInvitationRepository.save(invitation);
    }

    @Transactional
    public ClanInvitation respondToApplication(Long applicationId, Long requesterUserId, boolean accept, String reason) {
        ClanInvitation application = clanInvitationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Başvuru bulunamadı."));

        checkPermission(application.getClan().getId(), requesterUserId, List.of(ClanMemberRole.OWNER, ClanMemberRole.MANAGER), "Başvuruya cevap verme yetkiniz yok.");

        if (application.getStatus() != ClanInvitationStatus.PENDING) {
            throw new RuntimeException("Bu başvuru zaten cevaplanmış veya iptal edilmiş.");
        }
        if (application.getType() != ClanInvitationType.APPLICATION) {
            throw new RuntimeException("Bu bir başvuru değil, davet.");
        }

        if (accept) {
            application.setStatus(ClanInvitationStatus.ACCEPTED);
            addMemberToClan(application.getClan(), application.getUserId(), ClanMemberRole.MEMBER);
        } else {
            application.setStatus(ClanInvitationStatus.REJECTED);
            application.setReason(reason);
        }
        application.setRespondedAt(LocalDateTime.now());
        return clanInvitationRepository.save(application);
    }

    @Transactional
    public ClanInvitation cancelInvitation(Long invitationId, Long requesterUserId) {
        ClanInvitation invitation = clanInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Davet/Başvuru bulunamadı."));

        boolean isOwnerOrManager = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(invitation.getClan().getId(), requesterUserId)
                .map(m -> m.getRole() == ClanMemberRole.OWNER || m.getRole() == ClanMemberRole.MANAGER)
                .orElse(false);

        if (!invitation.getInviterId().equals(requesterUserId) && !isOwnerOrManager) {
            throw new RuntimeException("Bu işlemi iptal etme yetkiniz yok.");
        }
        if (invitation.getStatus() != ClanInvitationStatus.PENDING) {
            throw new RuntimeException("Sadece bekleyen işlemler iptal edilebilir.");
        }

        invitation.setStatus(ClanInvitationStatus.CANCELED);
        invitation.setRespondedAt(LocalDateTime.now());
        return clanInvitationRepository.save(invitation);
    }


    // --- Member Management ---

    @Transactional
    public ClanMember kickMember(Long clanMemberId, Long requesterUserId) {
        ClanMember memberToKick = clanMemberRepository.findByIdAndIsActiveTrue(clanMemberId)
                .orElseThrow(() -> new RuntimeException("Clan üyesi bulunamadı."));
        Clan clan = memberToKick.getClan();
        ClanMember requester = checkPermission(clan.getId(), requesterUserId, List.of(ClanMemberRole.OWNER, ClanMemberRole.MANAGER), "Üye atma yetkiniz yok.");

        if (requester.getRole() == ClanMemberRole.OWNER && memberToKick.getUserId().equals(requesterUserId)) {
            throw new RuntimeException("Kendinizi clan'dan atamazsınız. Clan'ı dağıtmalısınız.");
        }
        if (requester.getRole() == ClanMemberRole.MANAGER && memberToKick.getRole() != ClanMemberRole.MEMBER) {
            throw new RuntimeException("Sadece normal üyeleri atabilirsiniz.");
        }

        memberToKick.setIsActive(false);
        return clanMemberRepository.save(memberToKick);
    }

    @Transactional
    public ClanMember updateMemberRole(UpdateClanMemberRoleRequestBody request, Long requesterUserId) {
        ClanMember memberToUpdate = clanMemberRepository.findByIdAndIsActiveTrue(request.getClanMemberId())
                .orElseThrow(() -> new RuntimeException("Clan üyesi bulunamadı."));
        checkPermission(memberToUpdate.getClan().getId(), requesterUserId, List.of(ClanMemberRole.OWNER), "Sadece clan sahibi rol değiştirebilir.");

        if (memberToUpdate.getRole() == ClanMemberRole.OWNER) {
            throw new RuntimeException("Clan sahibinin rolü değiştirilemez.");
        }
        if (request.getNewRole() == ClanMemberRole.OWNER) {
            throw new RuntimeException("Yeni bir sahip atanamaz.");
        }

        memberToUpdate.setRole(request.getNewRole());
        return clanMemberRepository.save(memberToUpdate);
    }

    // --- Read Operations for Members and Invitations ---

    public List<ClanMember> getClanMembers(Long clanId) {
        return clanMemberRepository.findByClanIdAndIsActiveTrue(clanId);
    }

    public List<ClanMember> getUserClans(Long userId) {
        return clanMemberRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    public List<ClanInvitation> getPendingInvitationsForClan(Long clanId, Long requesterUserId) {
        checkPermission(clanId, requesterUserId, List.of(ClanMemberRole.OWNER, ClanMemberRole.MANAGER), "Yetkiniz yok.");
        return clanInvitationRepository.findByClanIdAndStatus(clanId, ClanInvitationStatus.PENDING);
    }

    public List<ClanInvitation> getPendingInvitationsForUser(Long userId) {
        return clanInvitationRepository.findByUserIdAndStatus(userId, ClanInvitationStatus.PENDING);
    }


    // --- Helper Methods ---

    private ClanMember checkPermission(Long clanId, Long userId, List<ClanMemberRole> requiredRoles, String errorMessage) {
        ClanMember member = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(clanId, userId)
                .orElseThrow(() -> new RuntimeException(errorMessage));
        if (!requiredRoles.contains(member.getRole())) {
            throw new RuntimeException(errorMessage);
        }
        return member;
    }

    private void addMemberToClan(Clan clan, Long userId, ClanMemberRole role) {
        if (clanMemberRepository.existsByUserIdAndCategoryIdAndIsActiveTrue(userId, clan.getCategory().getId())) {
            throw new RuntimeException("Kullanıcı zaten bu kategoride aktif bir clan üyesi.");
        }
        ClanMember newMember = new ClanMember();
        newMember.setClan(clan);
        newMember.setUserId(userId);
        newMember.setCategoryId(clan.getCategory().getId());
        newMember.setRole(role);
        newMember.setIsActive(true);
        clanMemberRepository.save(newMember);
    }
}
