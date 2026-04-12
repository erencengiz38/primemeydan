package com.meydan.meydan.service;

import com.meydan.meydan.exception.BaseException;
import com.meydan.meydan.exception.ErrorCode;
import com.meydan.meydan.models.entities.Category;
import com.meydan.meydan.models.entities.Clan;
import com.meydan.meydan.models.entities.ClanMember;
import com.meydan.meydan.models.entities.TournamentApplication;
import com.meydan.meydan.models.entities.TournamentApplicationPlayer;
import com.meydan.meydan.models.entities.TournamentStage;
import com.meydan.meydan.models.entities.Turnuva;
import com.meydan.meydan.models.enums.ClanMemberRole;
import com.meydan.meydan.models.enums.OrganizationRole;
import com.meydan.meydan.models.enums.ParticipantType;
import com.meydan.meydan.models.enums.TournamentApplicationStatus;
import com.meydan.meydan.models.enums.TournamentFormat;
import com.meydan.meydan.repository.*;
import com.meydan.meydan.request.Turnuva.AddTurnuvaRequestBody;
import com.meydan.meydan.request.Turnuva.ApplyToTournamentRequestBody;
import com.meydan.meydan.request.Turnuva.UpdateApplicationStatusRequestBody;
import com.meydan.meydan.request.Turnuva.UpdateTurnuvaRequestBody;
import com.meydan.meydan.request.Turnuva.RespondToTournamentInviteRequest;
import com.meydan.meydan.util.SocialMediaValidator;
import com.meydan.meydan.util.XssSanitizer;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnuvaService {

    private static final Logger logger = LoggerFactory.getLogger(TurnuvaService.class);

    private final TurnuvaRepository turnuvaRepository;
    private final CategoryRepository categoryRepository;
    private final ClanRepository clanRepository;
    private final ClanMemberRepository clanMemberRepository;
    private final TournamentApplicationRepository tournamentApplicationRepository;
    private final TournamentApplicationPlayerRepository tournamentApplicationPlayerRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final TournamentStageRepository tournamentStageRepository;
    private final ModelMapper modelMapper;
    private final XssSanitizer xssSanitizer;
    private final OrganizationService organizationService;
    private final SocialMediaValidator socialMediaValidator;

    // --- HELPER: GET CURRENT USER ID ---
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

    // --- ORTAK BOLA (YETKİ) KONTROLÜ METODU ---
    private void checkOrgPermission(Long organizationId) {
        Long userId = getCurrentUserId();
        boolean hasPermission = organizationMembershipRepository.existsByOrganizationIdAndUserIdAndRoleIn(
                organizationId, userId, Arrays.asList(OrganizationRole.OWNER, OrganizationRole.ADMIN));
        if (!hasPermission) {
            throw new BaseException(
                    ErrorCode.TRN_002,
                    "Bu işlem için organizasyon yetkiniz (OWNER/ADMIN) yok.",
                    HttpStatus.FORBIDDEN,
                    "OrgId: " + organizationId + ", UserId: " + userId
            );
        }
    }

    @Transactional
    public Turnuva createTurnuva(AddTurnuvaRequestBody request, Long organizationId) {
        // 1. Yetki Kontrolü
        checkOrgPermission(organizationId);

        // 2. Kategori Kontrolü
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Geçersiz kategori", HttpStatus.BAD_REQUEST, ""));

        // 3. TARİH MANTIK KONTROLÜ
        if (request.getRegistrationDeadline() != null && request.getStart_date() != null &&
                request.getRegistrationDeadline().after(request.getStart_date())) {
            throw new BaseException(ErrorCode.VAL_001, "Kayıt kapanış tarihi (Deadline), turnuva başlama tarihinden sonra olamaz!", HttpStatus.BAD_REQUEST, "");
        }

        // 4. MANUEL MAPPING
        Turnuva turnuva = new Turnuva();
        turnuva.setTitle(request.getTitle());
        turnuva.setDescription(request.getDescription());
        turnuva.setStart_date(request.getStart_date());
        turnuva.setFinish_date(request.getFinish_date());
        turnuva.setImageUrl(request.getImageUrl());
        turnuva.setReward_amount(request.getReward_amount());
        turnuva.setReward_currency(request.getReward_currency());
        turnuva.setPlayer_format(request.getPlayer_format());
        turnuva.setParticipantType(request.getParticipantType());
        turnuva.setTournamentFormat(request.getTournamentFormat());

        turnuva.setRegistrationDeadline(request.getRegistrationDeadline());
        turnuva.setMaxParticipants(request.getMaxParticipants());
        turnuva.setMinParticipants(request.getMinParticipants());
        turnuva.setTeamSize(request.getTeamSize());
        turnuva.setMatchCapacity(request.getMatchCapacity());
        turnuva.setCurrentParticipantsCount(0);

        turnuva.setId(null);
        turnuva.setCategory(category);
        turnuva.setOrganizationId(organizationId);
        turnuva.setIsActive(true);

        // XSS Temizlikleri
        if (turnuva.getTitle() != null) turnuva.setTitle(xssSanitizer.sanitizeAndLimit(turnuva.getTitle(), 200));
        if (turnuva.getDescription() != null) turnuva.setDescription(xssSanitizer.sanitizeAndLimit(turnuva.getDescription(), 1000));

        Turnuva savedTurnuva = turnuvaRepository.save(turnuva);
        turnuvaRepository.flush();

        // 5. Otomatik Aşama Oluşturma
        if (savedTurnuva.getTournamentFormat() == TournamentFormat.STAGE_BASED) {
            TournamentStage firstStage = new TournamentStage();
            firstStage.setTurnuva(savedTurnuva);
            firstStage.setName("1. Aşama");
            firstStage.setSequenceOrder(1);
            tournamentStageRepository.save(firstStage);
            logger.info("STAGE_BASED turnuva için ilk aşama oluşturuldu.");
        }

        return savedTurnuva;
    }

    public List<Turnuva> getAllTurnuvas() { return turnuvaRepository.findAll(); }
    public Page<Turnuva> getAllTurnuvasWithPagination(Pageable pageable) { return turnuvaRepository.findAll(pageable); }
    public List<Turnuva> getTurnuvasByOrganizationId(Long orgId) { return turnuvaRepository.findByOrganizationId(orgId); }
    public Page<Turnuva> getTurnuvasByOrganizationIdWithPagination(Long orgId, Pageable pageable) { return turnuvaRepository.findByOrganizationId(orgId, pageable); }

    @Transactional
    public Turnuva updateTurnuva(UpdateTurnuvaRequestBody request, Long organizationId) {
        checkOrgPermission(organizationId);
        Turnuva turnuva = turnuvaRepository.findById(request.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));

        if (!turnuva.getOrganizationId().equals(organizationId)) {
            throw new BaseException(ErrorCode.TRN_002, "IDOR Koruması: Yetkisiz erişim", HttpStatus.FORBIDDEN, "");
        }

        turnuva.setTitle(xssSanitizer.sanitizeAndLimit(request.getTitle(), 200));
        turnuva.setDescription(xssSanitizer.sanitizeAndLimit(request.getDescription(), 1000));
        turnuva.setStart_date(request.getStart_date());
        turnuva.setFinish_date(request.getFinish_date());
        return turnuvaRepository.save(turnuva);
    }

    @Transactional
    public Turnuva deleteTurnuva(Long id, Long organizationId) {
        checkOrgPermission(organizationId);
        Turnuva turnuva = turnuvaRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));
        turnuva.setIsActive(false);
        return turnuvaRepository.save(turnuva);
    }

    @Transactional
    public Turnuva restoreTurnuva(Long id, Long organizationId) {
        checkOrgPermission(organizationId);
        Turnuva turnuva = turnuvaRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));
        turnuva.setIsActive(true);
        return turnuvaRepository.save(turnuva);
    }

    @Transactional
    public Turnuva permanentlyDeleteTurnuva(Long id, Long organizationId) {
        checkOrgPermission(organizationId);
        Turnuva turnuva = turnuvaRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));
        turnuvaRepository.delete(turnuva);
        return turnuva;
    }

    // --- BAŞVURU MANTIKLARI ---
    @Transactional
    public TournamentApplication applyToTournament(ApplyToTournamentRequestBody request) {
        Long applicantUserId = getCurrentUserId();

        Turnuva tournament = turnuvaRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));

        if (!tournament.getIsActive()) {
            throw new BaseException(ErrorCode.VAL_001, "Bu turnuva aktif değil veya iptal edilmiş.", HttpStatus.BAD_REQUEST, "");
        }

        if (tournament.getRegistrationDeadline() != null && new Date().after(tournament.getRegistrationDeadline())) {
            throw new BaseException(ErrorCode.VAL_001, "Meydana giriş kapandı! Bu turnuva için kayıt süresi dolmuştur.", HttpStatus.BAD_REQUEST, "");
        }

        if (tournament.getMaxParticipants() != null && tournament.getCurrentParticipantsCount() >= tournament.getMaxParticipants()) {
            throw new BaseException(ErrorCode.VAL_001, "Meydan dolu! Bu turnuva maksimum kapasiteye ulaşmıştır.", HttpStatus.BAD_REQUEST, "");
        }

        if (tournamentApplicationRepository.existsByTournamentIdAndUserId(request.getTournamentId(), applicantUserId)) {
            throw new BaseException(ErrorCode.APP_001, "Bu turnuvaya zaten başvurdunuz.", HttpStatus.BAD_REQUEST, "");
        }

        TournamentApplication application = new TournamentApplication();
        application.setTournament(tournament);
        application.setUserId(applicantUserId);
        application.setStatus(TournamentApplicationStatus.PENDING);
        application.setIsCheckedIn(false); // Başlangıçta check-in false

        // ParticipantType'a göre validasyon
        if (tournament.getParticipantType() == ParticipantType.SOLO) {
            // SOLO turnuvalar için clan opsiyonel (representation için)
            if (request.getClanId() != null) {
                Clan clan = clanRepository.findById(request.getClanId())
                        .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Clan bulunamadı", HttpStatus.NOT_FOUND, ""));

                // Kullanıcının bu clan'da olup olmadığını kontrol et
                clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), applicantUserId)
                        .orElseThrow(() -> new BaseException(ErrorCode.AUTH_001, "Bu clan'ın üyesi değilsiniz", HttpStatus.FORBIDDEN, ""));

                application.setClan(clan);
            }
        } else if (tournament.getParticipantType() == ParticipantType.CLAN) {
            // CLAN turnuvalar için clan zorunlu
            if (request.getClanId() == null) {
                throw new BaseException(ErrorCode.APP_002, "Takım turnuvalarında clan seçimi zorunludur", HttpStatus.BAD_REQUEST, "");
            }

            Clan clan = clanRepository.findById(request.getClanId())
                    .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Clan bulunamadı", HttpStatus.NOT_FOUND, ""));

            if (!clan.getCategory().getId().equals(tournament.getCategory().getId())) {
                throw new BaseException(ErrorCode.APP_003, "Clan'ın kategorisi turnuva kategorisi ile uyuşmuyor", HttpStatus.BAD_REQUEST, "");
            }

            // Başvuranın yetkisi
            ClanMember member = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), applicantUserId)
                    .orElseThrow(() -> new BaseException(ErrorCode.AUTH_001, "Bu clan'ın üyesi değilsiniz", HttpStatus.FORBIDDEN, ""));

            if (member.getRole() != ClanMemberRole.OWNER && member.getRole() != ClanMemberRole.TEAM_CAPTAIN) {
                throw new BaseException(ErrorCode.APP_004, "Sadece clan sahibi veya takım kaptanı turnuvaya başvurabilir", HttpStatus.FORBIDDEN, "");
            }

            application.setClan(clan);
            
            // Roster Lock (Kadro Kilidi) - Başvuru anında seçili oyuncuları kopyala
            List<TournamentApplicationPlayer> roster = new ArrayList<>();

            if (request.getSelectedClanMemberIds() != null && !request.getSelectedClanMemberIds().isEmpty()) {
                // Seçili oyuncuları kopyala (Frontend'den User ID geliyor olarak varsayıyoruz ve ona göre ClanMember'ı buluyoruz)
                for (Long userIdToSelect : request.getSelectedClanMemberIds()) {
                    ClanMember selectedMember = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), userIdToSelect)
                            .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Seçilen oyuncu klanınızda bulunamadı (Kullanıcı ID: " + userIdToSelect + ")", HttpStatus.NOT_FOUND, ""));

                    TournamentApplicationPlayer player = new TournamentApplicationPlayer();
                    player.setTournamentApplication(application);
                    player.setClanMemberId(selectedMember.getId());
                    player.setUserId(selectedMember.getUserId());
                    roster.add(player);
                }
            } else {
                // Seçili oyuncu yoksa tüm aktif clan üyelerini kopyala (varsayılan davranış)
                List<ClanMember> activeMembers = clanMemberRepository.findByClanIdAndIsActiveTrue(request.getClanId());
                for (ClanMember activeMember : activeMembers) {
                    TournamentApplicationPlayer player = new TournamentApplicationPlayer();
                    player.setTournamentApplication(application);
                    player.setClanMemberId(activeMember.getId());
                    player.setUserId(activeMember.getUserId());
                    roster.add(player);
                }
            }
            
            application.setSelectedPlayers(roster);
        }

        return tournamentApplicationRepository.save(application);
    }

    @Transactional
    public TournamentApplication updateApplicationStatus(Long applicationId, UpdateApplicationStatusRequestBody request) {
        TournamentApplication application = tournamentApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Başvuru bulunamadı", HttpStatus.NOT_FOUND, ""));

        checkOrgPermission(application.getTournament().getOrganizationId());

        application.setStatus(request.getStatus());
        application.setRejectionReason(xssSanitizer.sanitizeAndLimit(request.getRejectionReason(), 500));
        application.setReviewedAt(java.time.LocalDateTime.now());

        if (request.getStatus() == TournamentApplicationStatus.APPROVED) {
            Turnuva tournament = application.getTournament();
            tournament.setCurrentParticipantsCount(tournament.getCurrentParticipantsCount() + 1);
            turnuvaRepository.save(tournament);
        }

        return tournamentApplicationRepository.save(application);
    }

    public List<TournamentApplication> getTournamentApplications(Long tournamentId) { 
        // Organizasyon yetkilisi olup olmadığını kontrol edelim
        Turnuva turnuva = turnuvaRepository.findById(tournamentId)
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));
        
        checkOrgPermission(turnuva.getOrganizationId());
        
        return tournamentApplicationRepository.findByTournamentId(tournamentId); 
    }

    public List<TournamentApplication> getUserApplications(Long userId) { return tournamentApplicationRepository.findByUserId(userId); }

    // --- CHECK-IN (YOKLAMA) SİSTEMİ ---
    @Transactional
    public TournamentApplication performCheckIn(Long tournamentId) {
        Long requesterUserId = getCurrentUserId();
        
        TournamentApplication application = tournamentApplicationRepository.findByTournamentIdAndUserId(tournamentId, requesterUserId)
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Bu turnuvaya ait bir başvurunuz bulunamadı.", HttpStatus.NOT_FOUND, ""));

        if (application.getStatus() != TournamentApplicationStatus.APPROVED) {
            throw new BaseException(ErrorCode.VAL_001, "Sadece onaylanmış başvurular yoklama (check-in) yapabilir.", HttpStatus.BAD_REQUEST, "");
        }

        Turnuva tournament = application.getTournament();
        long now = System.currentTimeMillis();
        long startTime = tournament.getStart_date().getTime();

        int checkInMinutes = 60;
        long checkInStartTime = startTime - (checkInMinutes * 60 * 1000L);

        if (now < checkInStartTime) {
            throw new BaseException(ErrorCode.VAL_001, "Yoklama süresi henüz başlamadı! Maça " + checkInMinutes + " dakika kala açılacaktır.", HttpStatus.BAD_REQUEST, "");
        }

        if (now > startTime) {
            throw new BaseException(ErrorCode.VAL_001, "Turnuva zaten başladı, check-in süresini kaçırdınız!", HttpStatus.BAD_REQUEST, "");
        }

        application.setIsCheckedIn(true);
        return tournamentApplicationRepository.save(application);
    }

    // --- ADMİN OVERRIDE (SÜRE UZATMA/KALDIRMA) ---
    @Transactional
    public Turnuva updateTournamentDeadline(Long tournamentId, Date newDeadline) {
        Turnuva turnuva = turnuvaRepository.findById(tournamentId)
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));

        checkOrgPermission(turnuva.getOrganizationId());
        turnuva.setRegistrationDeadline(newDeadline);

        logger.info("Turnuva (ID: {}) kayıt süresi güncellendi.", tournamentId);
        return turnuvaRepository.save(turnuva);
    }

    // --- VIP DAVET SİSTEMİ ---
    @Transactional
    public TournamentApplication inviteUserToTournament(Long tournamentId, Long targetUserId) {
        Turnuva turnuva = turnuvaRepository.findById(tournamentId)
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));

        checkOrgPermission(turnuva.getOrganizationId());

        if (turnuva.getMaxParticipants() != null && turnuva.getCurrentParticipantsCount() >= turnuva.getMaxParticipants()) {
            throw new BaseException(ErrorCode.VAL_001, "Kapasite dolu! Davet atmak için önce turnuva kapasitesini artırmalısınız.", HttpStatus.BAD_REQUEST, "");
        }

        if (tournamentApplicationRepository.findByTournamentIdAndUserId(tournamentId, targetUserId).isPresent()) {
            throw new BaseException(ErrorCode.VAL_001, "Bu kullanıcı zaten başvuru yapmış veya davet edilmiş.", HttpStatus.BAD_REQUEST, "");
        }

        TournamentApplication application = new TournamentApplication();
        application.setTournament(turnuva);
        application.setUserId(targetUserId);
        application.setStatus(TournamentApplicationStatus.INVITED);
        application.setIsCheckedIn(false); // Davet edilen de check-in yapmalı

        return tournamentApplicationRepository.save(application);
    }

    // --- OYUNCUNUN DAVETİ YANITLAMASI (ESNEK YAPI) ---
    @Transactional
    public TournamentApplication respondToTournamentInvite(Long applicationId, RespondToTournamentInviteRequest request) {
        Long targetUserId = getCurrentUserId();
        
        TournamentApplication application = tournamentApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Davet bulunamadı", HttpStatus.NOT_FOUND, ""));

        Turnuva turnuva = application.getTournament();

        // 1. Daveti yanıtlayanın yetkisi var mı kontrol et (Davet edilen kişi mi, yoksa org admin mi bypass yapıyor?)
        boolean isInvitee = application.getUserId().equals(targetUserId);
        boolean isOrgAdmin = false;
        
        if (!isInvitee) {
            // Davet edilen değil, o zaman admin mi diye bak
            isOrgAdmin = organizationMembershipRepository.existsByOrganizationIdAndUserIdAndRoleIn(
                    turnuva.getOrganizationId(), targetUserId, Arrays.asList(OrganizationRole.OWNER, OrganizationRole.ADMIN));
        }

        if (!isInvitee && !isOrgAdmin) {
            throw new BaseException(ErrorCode.VAL_001, "Bu daveti yanıtlama yetkiniz yok.", HttpStatus.FORBIDDEN, "");
        }

        if (application.getStatus() != TournamentApplicationStatus.INVITED) {
            throw new BaseException(ErrorCode.VAL_001, "Geçerli bir davet bulunmuyor veya daha önceden yanıtlanmış.", HttpStatus.BAD_REQUEST, "");
        }

        // 2. Duruma göre işlemler
        if (request.getStatus() == TournamentApplicationStatus.APPROVED) {
            // Eğer onaylarsa, kapasiteyi kontrol et
            if (turnuva.getMaxParticipants() != null && turnuva.getCurrentParticipantsCount() >= turnuva.getMaxParticipants()) {
                throw new BaseException(ErrorCode.VAL_001, "Malesef siz daveti kabul edene kadar turnuva kapasitesi doldu. Yedek olarak kaydolmayı deneyebilirsiniz.", HttpStatus.BAD_REQUEST, "");
            }
            
            // CLAN turnuvasıysa ekstra klan kadro kitleme işlemlerini yap (Apply'daki kural 2)
            if (turnuva.getParticipantType() == ParticipantType.SOLO) {
                if (request.getClanId() != null) {
                    Clan clan = clanRepository.findById(request.getClanId())
                            .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Clan bulunamadı", HttpStatus.NOT_FOUND, ""));

                    clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), application.getUserId())
                            .orElseThrow(() -> new BaseException(ErrorCode.AUTH_001, "Bu clan'ın üyesi değilsiniz", HttpStatus.FORBIDDEN, ""));

                    application.setClan(clan);
                }
            } else if (turnuva.getParticipantType() == ParticipantType.CLAN) {
                if (request.getClanId() == null) {
                    throw new BaseException(ErrorCode.APP_002, "Takım turnuvalarında clan seçimi zorunludur", HttpStatus.BAD_REQUEST, "");
                }

                Clan clan = clanRepository.findById(request.getClanId())
                        .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Clan bulunamadı", HttpStatus.NOT_FOUND, ""));

                if (!clan.getCategory().getId().equals(turnuva.getCategory().getId())) {
                    throw new BaseException(ErrorCode.APP_003, "Clan'ın kategorisi turnuva kategorisi ile uyuşmuyor", HttpStatus.BAD_REQUEST, "");
                }

                ClanMember member = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), application.getUserId())
                        .orElseThrow(() -> new BaseException(ErrorCode.AUTH_001, "Bu clan'ın üyesi değilsiniz", HttpStatus.FORBIDDEN, ""));

                // Admin bypass yapmıyorsa, kabul edenin klan yetkilisi olması lazım
                if (!isOrgAdmin && member.getRole() != ClanMemberRole.OWNER && member.getRole() != ClanMemberRole.TEAM_CAPTAIN) {
                    throw new BaseException(ErrorCode.APP_004, "Sadece clan sahibi veya takım kaptanı daveti kabul edip takımı turnuvaya sokabilir", HttpStatus.FORBIDDEN, "");
                }

                application.setClan(clan);
                
                List<TournamentApplicationPlayer> roster = new ArrayList<>();

                if (request.getSelectedClanMemberIds() != null && !request.getSelectedClanMemberIds().isEmpty()) {
                    for (Long userIdToSelect : request.getSelectedClanMemberIds()) {
                        ClanMember selectedMember = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), userIdToSelect)
                                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Seçilen oyuncu klanınızda bulunamadı (Kullanıcı ID: " + userIdToSelect + ")", HttpStatus.NOT_FOUND, ""));

                        TournamentApplicationPlayer player = new TournamentApplicationPlayer();
                        player.setTournamentApplication(application);
                        player.setClanMemberId(selectedMember.getId());
                        player.setUserId(selectedMember.getUserId());
                        roster.add(player);
                    }
                } else {
                    List<ClanMember> activeMembers = clanMemberRepository.findByClanIdAndIsActiveTrue(request.getClanId());
                    for (ClanMember activeMember : activeMembers) {
                        TournamentApplicationPlayer player = new TournamentApplicationPlayer();
                        player.setTournamentApplication(application);
                        player.setClanMemberId(activeMember.getId());
                        player.setUserId(activeMember.getUserId());
                        roster.add(player);
                    }
                }
                
                application.setSelectedPlayers(roster);
            }

            application.setStatus(TournamentApplicationStatus.APPROVED);
            turnuva.setCurrentParticipantsCount(turnuva.getCurrentParticipantsCount() + 1);
            turnuvaRepository.save(turnuva);
            
        } else if (request.getStatus() == TournamentApplicationStatus.REJECTED || request.getStatus() == TournamentApplicationStatus.SUBSTITUTE) {
            // Reddetme veya yedek olma durumu
            application.setStatus(request.getStatus());
            
            // Sebebi güvenli bir şekilde kaydet (XSS Koruması)
            if (request.getReason() != null) {
                application.setRejectionReason(xssSanitizer.sanitizeAndLimit(request.getReason(), 500));
            }
            
        } else {
            throw new BaseException(ErrorCode.VAL_001, "Geçersiz yanıt durumu. Yalnızca APPROVED, REJECTED veya SUBSTITUTE seçilebilir.", HttpStatus.BAD_REQUEST, "");
        }

        return tournamentApplicationRepository.save(application);
    }
}
