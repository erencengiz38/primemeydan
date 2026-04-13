package com.meydan.meydan.service;

import com.meydan.meydan.exception.BaseException;
import com.meydan.meydan.exception.ErrorCode;
import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.models.enums.ClanInvitationType;
import com.meydan.meydan.models.enums.ClanMemberRole;
import com.meydan.meydan.repository.CategoryRepository;
import com.meydan.meydan.repository.ClanInvitationRepository;
import com.meydan.meydan.repository.ClanMemberRepository;
import com.meydan.meydan.repository.ClanRepository;
import com.meydan.meydan.request.Clan.AddClanRequestBody;
import com.meydan.meydan.request.Clan.RespondToInvitationRequest;
import com.meydan.meydan.request.Clan.UpdateClanMemberRoleRequestBody;
import com.meydan.meydan.util.XssSanitizer;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import com.meydan.meydan.models.enums.ClanInvitationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final XssSanitizer xssSanitizer;

    // --- Helper: Get Current User ID ---
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BaseException(
                    ErrorCode.AUTH_002,
                    "Kullanıcı oturumu bulunamadı. Lütfen giriş yapın.",
                    HttpStatus.UNAUTHORIZED,
                    "Authentication: " + authentication
            );
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BaseException(
                    ErrorCode.AUTH_003,
                    "Kullanıcı kimliği doğrulanamadı.",
                    HttpStatus.UNAUTHORIZED,
                    "Principal: " + authentication.getPrincipal()
            );
        }
    }

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
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SYS_001, // Veya CLAN_001 eklenebilir
                        "Clan bulunamadı",
                        HttpStatus.NOT_FOUND,
                        "Clan ID: " + id
                ));
    }

    // --- Clan Create Operation ---
    @Transactional
    public Clan createClan(AddClanRequestBody request) {
        Long creatorUserId = getCurrentUserId();

        // 1. Zaten üye mi kontrolü
        if (clanMemberRepository.existsActiveMemberInActiveClan(creatorUserId, request.getCategoryId())) {
            throw new BaseException(
                    ErrorCode.VAL_001,
                    "Bu oyun kategorisinde zaten bir clan üyesisiniz.",
                    HttpStatus.BAD_REQUEST,
                    "User ID: " + creatorUserId + ", Category ID: " + request.getCategoryId()
            );
        }

        // 2. İsim kontrolü
        if (clanRepository.existsByCategoryIdAndNameAndIsActiveTrue(request.getCategoryId(), request.getName())) {
            throw new BaseException(
                    ErrorCode.VAL_001,
                    "Bu kategoride aynı isimde clan zaten var.",
                    HttpStatus.BAD_REQUEST,
                    "Category ID: " + request.getCategoryId() + ", Clan Name: " + request.getName()
            );
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BaseException(
                        ErrorCode.VAL_001,
                        "Kategori bulunamadı.",
                        HttpStatus.NOT_FOUND,
                        "Category ID: " + request.getCategoryId()
                ));

        // XSS Sanitization
        request.setName(xssSanitizer.sanitizeAndLimit(request.getName(), 100));
        if (request.getDescription() != null) {
            request.setDescription(xssSanitizer.sanitizeAndLimit(request.getDescription(), 1000));
        }

        Clan clan = modelMapper.map(request, Clan.class);
        clan.setId(null);
        clan.setCategory(category);
        clan.setIsActive(true);

        Clan savedClan = clanRepository.save(clan);

        addMemberToClan(savedClan, creatorUserId, ClanMemberRole.OWNER);

        return savedClan;
    }

    // --- Invitation and Application System ---

    @Transactional
    public ClanInvitation inviteToClan(Long clanId, Long userIdToInvite) {
        Long requesterUserId = getCurrentUserId();
        Clan clan = getClanById(clanId);
        checkPermission(clanId, requesterUserId, List.of(ClanMemberRole.OWNER, ClanMemberRole.MANAGER), "Davet gönderme yetkiniz yok.");

        if (clanMemberRepository.existsActiveMemberInActiveClan(userIdToInvite, clan.getCategory().getId())) {
            throw new BaseException(
                    ErrorCode.VAL_001,
                    "Davet edilen kullanıcı zaten bu kategoride aktif bir clan üyesi.",
                    HttpStatus.BAD_REQUEST,
                    "Invited User ID: " + userIdToInvite + ", Category ID: " + clan.getCategory().getId()
            );
        }
        if (clanInvitationRepository.findByClanIdAndUserIdAndStatus(clanId, userIdToInvite, ClanInvitationStatus.PENDING).isPresent()) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Bu kullanıcıya zaten bekleyen bir davet/başvuru var.",
                    HttpStatus.BAD_REQUEST,
                    "Clan ID: " + clanId + ", Invited User ID: " + userIdToInvite
            );
        }

        ClanInvitation invitation = new ClanInvitation();
        invitation.setClan(clan);
        invitation.setUserId(userIdToInvite);
        invitation.setInviterId(requesterUserId);
        invitation.setType(ClanInvitationType.INVITATION);
        return clanInvitationRepository.save(invitation);
    }

    @Transactional
    public ClanInvitation applyToClan(Long clanId) {
        Long applicantUserId = getCurrentUserId();
        Clan clan = getClanById(clanId);

        if (clanMemberRepository.existsActiveMemberInActiveClan(applicantUserId, clan.getCategory().getId())) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Bu oyun kategorisinde zaten aktif bir clan üyesisiniz.",
                    HttpStatus.BAD_REQUEST,
                    "Applicant User ID: " + applicantUserId + ", Category ID: " + clan.getCategory().getId()
            );
        }
        if (clanInvitationRepository.findByClanIdAndUserIdAndStatus(clanId, applicantUserId, ClanInvitationStatus.PENDING).isPresent()) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Bu clana zaten bekleyen bir başvurun/davetin var.",
                    HttpStatus.BAD_REQUEST,
                    "Clan ID: " + clanId + ", Applicant User ID: " + applicantUserId
            );
        }

        ClanInvitation application = new ClanInvitation();
        application.setClan(clan);
        application.setUserId(applicantUserId);
        application.setInviterId(applicantUserId); // Applicant is the initiator
        application.setType(ClanInvitationType.APPLICATION);
        return clanInvitationRepository.save(application);
    }

    @Transactional
    public ClanInvitation respondToInvitation(Long invitationId, boolean accept, String reason) {
        Long respondingUserId = getCurrentUserId();
        ClanInvitation invitation = clanInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SYS_001,
                        "Davet bulunamadı.",
                        HttpStatus.NOT_FOUND,
                        "Invitation ID: " + invitationId
                ));

        if (!invitation.getUserId().equals(respondingUserId)) {
             throw new BaseException(
                    ErrorCode.AUTH_001,
                    "Bu davete cevap verme yetkiniz yok.",
                    HttpStatus.FORBIDDEN,
                    "Responding User ID: " + respondingUserId + ", Invitation Target User ID: " + invitation.getUserId()
            );
        }
        if (invitation.getStatus() != ClanInvitationStatus.PENDING) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Bu davet zaten cevaplanmış veya iptal edilmiş.",
                    HttpStatus.BAD_REQUEST,
                    "Invitation Status: " + invitation.getStatus()
            );
        }
        if (invitation.getType() != ClanInvitationType.INVITATION) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Bu bir davet değil, başvuru.",
                    HttpStatus.BAD_REQUEST,
                    "Invitation Type: " + invitation.getType()
            );
        }

        if (accept) {
            invitation.setStatus(ClanInvitationStatus.ACCEPTED);
            addMemberToClan(invitation.getClan(), invitation.getUserId(), ClanMemberRole.MEMBER);
        } else {
            invitation.setStatus(ClanInvitationStatus.REJECTED);
            invitation.setReason(xssSanitizer.sanitizeAndLimit(reason, 255));
        }
        invitation.setRespondedAt(LocalDateTime.now());
        return clanInvitationRepository.save(invitation);
    }

    @Transactional
    public ClanInvitation respondToApplication(Long applicationId, boolean accept, String reason) {
        Long requesterUserId = getCurrentUserId();
        ClanInvitation application = clanInvitationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SYS_001,
                        "Başvuru bulunamadı.",
                        HttpStatus.NOT_FOUND,
                        "Application ID: " + applicationId
                ));

        checkPermission(application.getClan().getId(), requesterUserId, List.of(ClanMemberRole.OWNER, ClanMemberRole.MANAGER), "Başvuruya cevap verme yetkiniz yok.");

        if (application.getStatus() != ClanInvitationStatus.PENDING) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Bu başvuru zaten cevaplanmış veya iptal edilmiş.",
                    HttpStatus.BAD_REQUEST,
                    "Application Status: " + application.getStatus()
            );
        }
        if (application.getType() != ClanInvitationType.APPLICATION) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Bu bir başvuru değil, davet.",
                    HttpStatus.BAD_REQUEST,
                    "Application Type: " + application.getType()
            );
        }

        if (accept) {
            application.setStatus(ClanInvitationStatus.ACCEPTED);
            addMemberToClan(application.getClan(), application.getUserId(), ClanMemberRole.MEMBER);
        } else {
            application.setStatus(ClanInvitationStatus.REJECTED);
            application.setReason(xssSanitizer.sanitizeAndLimit(reason, 255));
        }
        application.setRespondedAt(LocalDateTime.now());
        return clanInvitationRepository.save(application);
    }

    @Transactional
    public ClanInvitation cancelInvitation(Long invitationId) {
        Long requesterUserId = getCurrentUserId();
        ClanInvitation invitation = clanInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SYS_001,
                        "Davet/Başvuru bulunamadı.",
                        HttpStatus.NOT_FOUND,
                        "Invitation ID: " + invitationId
                ));

        boolean isOwnerOrManager = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(invitation.getClan().getId(), requesterUserId)
                .map(m -> m.getRole() == ClanMemberRole.OWNER || m.getRole() == ClanMemberRole.MANAGER)
                .orElse(false);

        if (!invitation.getInviterId().equals(requesterUserId) && !isOwnerOrManager) {
             throw new BaseException(
                    ErrorCode.AUTH_001,
                    "Bu işlemi iptal etme yetkiniz yok.",
                    HttpStatus.FORBIDDEN,
                    "Requester ID: " + requesterUserId + ", Inviter ID: " + invitation.getInviterId()
            );
        }
        if (invitation.getStatus() != ClanInvitationStatus.PENDING) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Sadece bekleyen işlemler iptal edilebilir.",
                    HttpStatus.BAD_REQUEST,
                    "Invitation Status: " + invitation.getStatus()
            );
        }

        invitation.setStatus(ClanInvitationStatus.CANCELED);
        invitation.setRespondedAt(LocalDateTime.now());
        return clanInvitationRepository.save(invitation);
    }


    // --- Member Management ---

    @Transactional
    public ClanMember kickMember(Long clanMemberId) {
        Long requesterUserId = getCurrentUserId();
        ClanMember memberToKick = clanMemberRepository.findByIdAndIsActiveTrue(clanMemberId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SYS_001,
                        "Clan üyesi bulunamadı.",
                        HttpStatus.NOT_FOUND,
                        "Clan Member ID: " + clanMemberId
                ));
        Clan clan = memberToKick.getClan();
        ClanMember requester = checkPermission(clan.getId(), requesterUserId, List.of(ClanMemberRole.OWNER, ClanMemberRole.MANAGER), "Üye atma yetkiniz yok.");

        if (requester.getRole() == ClanMemberRole.OWNER && memberToKick.getUserId().equals(requesterUserId)) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Kendinizi clan'dan atamazsınız. Ayrılma işlemini kullanın.",
                    HttpStatus.BAD_REQUEST,
                    "User ID: " + requesterUserId
            );
        }
        if (requester.getRole() == ClanMemberRole.MANAGER && memberToKick.getRole() != ClanMemberRole.MEMBER) {
             throw new BaseException(
                    ErrorCode.AUTH_001,
                    "Sadece normal üyeleri atabilirsiniz.",
                    HttpStatus.FORBIDDEN,
                    "Requester Role: " + requester.getRole() + ", Target Role: " + memberToKick.getRole()
            );
        }

        memberToKick.setIsActive(false);
        return clanMemberRepository.save(memberToKick);
    }

    @Transactional
    public ClanMember updateMemberRole(UpdateClanMemberRoleRequestBody request) {
        Long requesterUserId = getCurrentUserId();
        ClanMember memberToUpdate = clanMemberRepository.findByIdAndIsActiveTrue(request.getClanMemberId())
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SYS_001,
                        "Clan üyesi bulunamadı.",
                        HttpStatus.NOT_FOUND,
                        "Clan Member ID: " + request.getClanMemberId()
                ));
        checkPermission(memberToUpdate.getClan().getId(), requesterUserId, List.of(ClanMemberRole.OWNER), "Sadece clan sahibi rol değiştirebilir.");

        if (memberToUpdate.getRole() == ClanMemberRole.OWNER) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Clan sahibinin rolü değiştirilemez. Yetki devri yapın.",
                    HttpStatus.BAD_REQUEST,
                    "Target Member Role: " + memberToUpdate.getRole()
            );
        }
        if (request.getNewRole() == ClanMemberRole.OWNER) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Yeni bir sahip atanamaz. Yetki devri yapın.",
                    HttpStatus.BAD_REQUEST,
                    "New Role: " + request.getNewRole()
            );
        }

        memberToUpdate.setRole(request.getNewRole());
        return clanMemberRepository.save(memberToUpdate);
    }

    // YENİ KURAL 3: Sahipsiz Klan Koruması
    @Transactional
    public void leaveClan(Long clanId) {
        Long userId = getCurrentUserId();
        Clan clan = getClanById(clanId);

        ClanMember leavingMember = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(clanId, userId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SYS_001,
                        "Bu clanın üyesi değilsiniz.",
                        HttpStatus.BAD_REQUEST,
                        "Clan ID: " + clanId + ", User ID: " + userId
                ));

        List<ClanMember> activeMembers = clanMemberRepository.findByClanIdAndIsActiveTrue(clanId);

        if (leavingMember.getRole() == ClanMemberRole.OWNER) {
            if (activeMembers.size() > 1) {
                // Klanda başkaları varken OWNER ayrılamaz, devretmesi lazım
                throw new BaseException(
                        ErrorCode.VAL_001,
                        "Klanda başka üyeler varken clan sahibi ayrılamaz. Lütfen önce yetkiyi başkasına devredin.",
                        HttpStatus.BAD_REQUEST,
                        "Clan ID: " + clanId + ", Active Members: " + activeMembers.size()
                );
            } else {
                // Klanda tek kişi (OWNER) kalmışsa ve ayrılıyorsa klanı soft delete yap
                clan.setIsActive(false);
                clanRepository.save(clan);
            }
        }

        // Üyeyi soft delete yap
        leavingMember.setIsActive(false);
        clanMemberRepository.save(leavingMember);
    }


    // --- Read Operations for Members and Invitations ---

    public List<ClanMember> getClanMembers(Long clanId) {
        return clanMemberRepository.findByClanIdAndIsActiveTrue(clanId);
    }

    public List<ClanMember> getUserClans(Long userId) {
        return clanMemberRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    public List<ClanInvitation> getPendingInvitationsForClan(Long clanId) {
        Long requesterUserId = getCurrentUserId();
        checkPermission(clanId, requesterUserId, List.of(ClanMemberRole.OWNER, ClanMemberRole.MANAGER), "Yetkiniz yok.");
        return clanInvitationRepository.findByClanIdAndStatus(clanId, ClanInvitationStatus.PENDING);
    }

    public List<ClanInvitation> getPendingInvitationsForUser(Long userId) {
        return clanInvitationRepository.findByUserIdAndStatus(userId, ClanInvitationStatus.PENDING);
    }

    public List<ClanInvitation> getClanApplications(Long clanId) {
        Long requesterUserId = getCurrentUserId();
        checkPermission(clanId, requesterUserId, List.of(ClanMemberRole.OWNER, ClanMemberRole.MANAGER), "Başvuruları görme yetkiniz yok.");
        return clanInvitationRepository.findByClanIdAndType(clanId, ClanInvitationType.APPLICATION);
    }


    // --- Helper Methods ---

    private ClanMember checkPermission(Long clanId, Long userId, List<ClanMemberRole> requiredRoles, String errorMessage) {
        ClanMember member = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(clanId, userId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.AUTH_001,
                        errorMessage,
                        HttpStatus.FORBIDDEN,
                        "Clan ID: " + clanId + ", User ID: " + userId
                ));
        if (!requiredRoles.contains(member.getRole())) {
             throw new BaseException(
                    ErrorCode.AUTH_001,
                    errorMessage,
                    HttpStatus.FORBIDDEN,
                    "User Role: " + member.getRole() + ", Required Roles: " + requiredRoles
            );
        }
        return member;
    }

    private void addMemberToClan(Clan clan, Long userId, ClanMemberRole role) {
        // GÜNCELLEME: Sadece üyenin değil, klanın da aktifliğini kontrol et
        if (clanMemberRepository.existsActiveMemberInActiveClan(userId, clan.getCategory().getId())) {
             throw new BaseException(
                    ErrorCode.VAL_001,
                    "Kullanıcı zaten bu kategoride aktif bir clan üyesi.",
                    HttpStatus.BAD_REQUEST,
                    "User ID: " + userId + ", Category ID: " + clan.getCategory().getId()
            );
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
