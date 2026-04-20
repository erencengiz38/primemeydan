package com.meydan.meydan.service;

import com.meydan.meydan.exception.BaseException;
import com.meydan.meydan.exception.ErrorCode;
import com.meydan.meydan.exception.PlayerAlreadyBusyException;
import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.models.enums.*;
import com.meydan.meydan.repository.*;
import com.meydan.meydan.request.Turnuva.*;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final OrganizationQuotaRepository organizationQuotaRepository; 
    private final TournamentStageRepository tournamentStageRepository;
    private final WalletService walletService; 
    private final ModelMapper modelMapper;
    private final XssSanitizer xssSanitizer;
    private final SocialMediaValidator socialMediaValidator;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BaseException(ErrorCode.AUTH_002, "Kullanıcı oturumu bulunamadı. Lütfen giriş yapın.", HttpStatus.UNAUTHORIZED, "Authentication: " + authentication);
        }

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BaseException(ErrorCode.AUTH_003, "Kullanıcı kimliği doğrulanamadı.", HttpStatus.UNAUTHORIZED, "Principal: " + authentication.getPrincipal());
        }
    }

    private void checkOrgPermission(Long organizationId) {
        Long userId = getCurrentUserId();
        
        boolean isDirectOwner = organizationId.equals(userId);
        boolean hasPermission = isDirectOwner || organizationMembershipRepository.existsByOrganizationIdAndUserIdAndRoleIn(
                organizationId, userId, Arrays.asList(OrganizationRole.OWNER, OrganizationRole.ADMIN));
                
        if (!hasPermission) {
            throw new BaseException(ErrorCode.TRN_002, "Bu işlem için organizasyon yetkiniz (OWNER/ADMIN) yok.", HttpStatus.FORBIDDEN, "OrgId: " + organizationId + ", UserId: " + userId);
        }
    }

    @Transactional
    public Turnuva createTurnuva(AddTurnuvaRequestBody request, Long organizationId) {
        checkOrgPermission(organizationId);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Geçersiz kategori", HttpStatus.BAD_REQUEST, ""));

        if (request.getRegistrationDeadline() != null && request.getStart_date() != null &&
                request.getRegistrationDeadline().after(request.getStart_date())) {
            throw new BaseException(ErrorCode.VAL_001, "Kayıt kapanış tarihi (Deadline), turnuva başlama tarihinden sonra olamaz!", HttpStatus.BAD_REQUEST, "");
        }

        if (request.getMinTeamSize() == null || request.getMaxTeamSize() == null || request.getMinTeamSize() < 1 || request.getMaxTeamSize() < request.getMinTeamSize()) {
            throw new BaseException(ErrorCode.VAL_001, "Geçersiz takım boyutu. MinTeamSize ve MaxTeamSize değerlerini doğru giriniz.", HttpStatus.BAD_REQUEST, "");
        }

        OrganizationQuota quota = organizationQuotaRepository.findById(organizationId)
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Organizasyon kotası bulunamadı. Lütfen organizasyon ayarlarını kontrol edin.", HttpStatus.BAD_REQUEST, "OrgId: " + organizationId));

        BigDecimal rewardAmount = BigDecimal.valueOf(request.getReward_amount() != null ? request.getReward_amount() : 0.0);
        BigDecimal availableQuota = quota.getWeeklyLimit().subtract(quota.getCurrentSpent());

        if (rewardAmount.compareTo(availableQuota) > 0) {
            throw new BaseException(ErrorCode.VAL_001, "Organizasyon haftalık ödül kotanız aşıldı! Kalan Kotanız: " + availableQuota + " MC", HttpStatus.BAD_REQUEST, "Requested: " + rewardAmount);
        }

        quota.setCurrentSpent(quota.getCurrentSpent().add(rewardAmount));
        organizationQuotaRepository.save(quota);

        Turnuva turnuva = new Turnuva();
        turnuva.setTitle(request.getTitle());
        turnuva.setDescription(request.getDescription());
        turnuva.setStart_date(request.getStart_date());
        turnuva.setFinish_date(request.getFinish_date());
        turnuva.setImageUrl(request.getImageUrl());
        turnuva.setReward_amount(request.getReward_amount());
        turnuva.setReward_currency(request.getReward_currency() != null ? request.getReward_currency() : "MEYDAN_COIN"); 
        turnuva.setPlayer_format(request.getPlayer_format());
        turnuva.setTournamentFormat(request.getTournamentFormat());
        turnuva.setRegistrationDeadline(request.getRegistrationDeadline());
        turnuva.setMaxParticipants(request.getMaxParticipants());
        turnuva.setMinParticipants(request.getMinParticipants());
        turnuva.setMinTeamSize(request.getMinTeamSize());
        turnuva.setMaxTeamSize(request.getMaxTeamSize());
        turnuva.setMatchCapacity(request.getMatchCapacity());
        turnuva.setCurrentParticipantsCount(0);

        turnuva.setId(null);
        turnuva.setCategory(category);
        turnuva.setOrganizationId(organizationId);
        
        turnuva.setIsActive(true);
        turnuva.setApprovalStatus(ApplicationStatus.PENDING);

        if (turnuva.getTitle() != null) turnuva.setTitle(xssSanitizer.sanitizeAndLimit(turnuva.getTitle(), 200));
        if (turnuva.getDescription() != null) turnuva.setDescription(xssSanitizer.sanitizeAndLimit(turnuva.getDescription(), 1000));

        Turnuva savedTurnuva = turnuvaRepository.save(turnuva);
        turnuvaRepository.flush();

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

    public List<Turnuva> getAllTurnuvas() { 
        return turnuvaRepository.findByApprovalStatusAndIsActiveTrue(ApplicationStatus.APPROVED); 
    }
    
    public Page<Turnuva> getAllTurnuvasWithPagination(Pageable pageable) { 
        return turnuvaRepository.findByApprovalStatusAndIsActiveTrue(ApplicationStatus.APPROVED, pageable); 
    }
    
    public List<Turnuva> getTurnuvasByOrganizationId(Long orgId) { return turnuvaRepository.findByOrganizationId(orgId); }
    public Page<Turnuva> getTurnuvasByOrganizationIdWithPagination(Long orgId, Pageable pageable) { return turnuvaRepository.findByOrganizationId(orgId, pageable); }

    public Turnuva getTurnuvaById(Long id) {
        return turnuvaRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));
    }

    public List<Turnuva> getPendingTurnuvas() {
        return turnuvaRepository.findByApprovalStatusAndIsActiveTrue(ApplicationStatus.PENDING);
    }
    
    @Transactional
    public Turnuva approveTurnuva(Long turnuvaId, String adminNotes) {
        Turnuva turnuva = getTurnuvaById(turnuvaId);
        
        if (turnuva.getApprovalStatus() != ApplicationStatus.PENDING) {
            throw new BaseException(ErrorCode.VAL_001, "Bu turnuva zaten değerlendirilmiş.", HttpStatus.BAD_REQUEST, "");
        }
        
        turnuva.setApprovalStatus(ApplicationStatus.APPROVED);
        turnuva.setAdminNotes(adminNotes);
        return turnuvaRepository.save(turnuva);
    }
    
    @Transactional
    public Turnuva rejectTurnuva(Long turnuvaId, String adminNotes) {
        Turnuva turnuva = getTurnuvaById(turnuvaId);
        
        if (turnuva.getApprovalStatus() != ApplicationStatus.PENDING) {
            throw new BaseException(ErrorCode.VAL_001, "Bu turnuva zaten değerlendirilmiş.", HttpStatus.BAD_REQUEST, "");
        }
        
        turnuva.setApprovalStatus(ApplicationStatus.REJECTED);
        turnuva.setAdminNotes(adminNotes);
        turnuva.setIsActive(false); 
        
        OrganizationQuota quota = organizationQuotaRepository.findById(turnuva.getOrganizationId())
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Kota bulunamadı", HttpStatus.NOT_FOUND, ""));
        
        BigDecimal rewardAmount = BigDecimal.valueOf(turnuva.getReward_amount() != null ? turnuva.getReward_amount() : 0.0);
        
        // Güvenli iade: Asla eksiye düşmez (0'da durur)
        quota.setCurrentSpent(quota.getCurrentSpent().subtract(rewardAmount).max(BigDecimal.ZERO));
        organizationQuotaRepository.save(quota);
        
        return turnuvaRepository.save(turnuva);
    }

    @Transactional
    public Turnuva updateTurnuva(UpdateTurnuvaRequestBody request, Long organizationId) {
        checkOrgPermission(organizationId);
        Turnuva turnuva = turnuvaRepository.findById(request.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));

        if (!turnuva.getOrganizationId().equals(organizationId)) {
            throw new BaseException(ErrorCode.TRN_002, "IDOR Koruması: Yetkisiz erişim", HttpStatus.FORBIDDEN, "");
        }

        if (request.getMinTeamSize() != null && request.getMaxTeamSize() != null) {
            if (request.getMinTeamSize() < 1 || request.getMaxTeamSize() < request.getMinTeamSize()) {
                throw new BaseException(ErrorCode.VAL_001, "Geçersiz takım boyutu. MinTeamSize ve MaxTeamSize değerlerini doğru giriniz.", HttpStatus.BAD_REQUEST, "");
            }
            turnuva.setMinTeamSize(request.getMinTeamSize());
            turnuva.setMaxTeamSize(request.getMaxTeamSize());
        }

        turnuva.setTitle(xssSanitizer.sanitizeAndLimit(request.getTitle(), 200));
        turnuva.setDescription(xssSanitizer.sanitizeAndLimit(request.getDescription(), 1000));
        turnuva.setStart_date(request.getStart_date());
        turnuva.setFinish_date(request.getFinish_date());
        
        if (request.getMatchCapacity() != null) turnuva.setMatchCapacity(request.getMatchCapacity());
        if (request.getMinParticipants() != null) turnuva.setMinParticipants(request.getMinParticipants());
        if (request.getMaxParticipants() != null) turnuva.setMaxParticipants(request.getMaxParticipants());

        return turnuvaRepository.save(turnuva);
    }

    @Transactional
    public Turnuva deleteTurnuva(Long id, Long organizationId) {
        checkOrgPermission(organizationId);
        Turnuva turnuva = turnuvaRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));
        
        if (turnuva.getIsActive() && turnuva.getApprovalStatus() != ApplicationStatus.REJECTED) {
            OrganizationQuota quota = organizationQuotaRepository.findById(turnuva.getOrganizationId())
                    .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Kota bulunamadı", HttpStatus.NOT_FOUND, ""));
            
            BigDecimal rewardAmount = BigDecimal.valueOf(turnuva.getReward_amount() != null ? turnuva.getReward_amount() : 0.0);
            
            // Güvenli iade: Asla eksiye düşmez
            quota.setCurrentSpent(quota.getCurrentSpent().subtract(rewardAmount).max(BigDecimal.ZERO));
            organizationQuotaRepository.save(quota);
        }
        
        turnuva.setIsActive(false);
        return turnuvaRepository.save(turnuva);
    }

    @Transactional
    public Turnuva restoreTurnuva(Long id, Long organizationId) {
        checkOrgPermission(organizationId);
        Turnuva turnuva = turnuvaRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));
        turnuva.setIsActive(true);
        turnuva.setApprovalStatus(ApplicationStatus.PENDING);
        
        OrganizationQuota quota = organizationQuotaRepository.findById(turnuva.getOrganizationId())
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Kota bulunamadı", HttpStatus.NOT_FOUND, ""));
                
        BigDecimal rewardAmount = BigDecimal.valueOf(turnuva.getReward_amount() != null ? turnuva.getReward_amount() : 0.0);
        BigDecimal availableQuota = quota.getWeeklyLimit().subtract(quota.getCurrentSpent());

        if (rewardAmount.compareTo(availableQuota) > 0) {
            throw new BaseException(ErrorCode.VAL_001, "Organizasyon haftalık ödül kotanız aşıldı! Restorasyon başarısız.", HttpStatus.BAD_REQUEST, "Requested: " + rewardAmount);
        }

        quota.setCurrentSpent(quota.getCurrentSpent().add(rewardAmount));
        organizationQuotaRepository.save(quota);
        
        return turnuvaRepository.save(turnuva);
    }

    @Transactional
    public Turnuva permanentlyDeleteTurnuva(Long id, Long organizationId) {
        checkOrgPermission(organizationId);
        Turnuva turnuva = turnuvaRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));
        turnuvaRepository.delete(turnuva);
        return turnuva;
    }

    @Transactional
    public TournamentApplication applyToTournament(ApplyToTournamentRequestBody request) {
        Long applicantUserId = getCurrentUserId();

        Turnuva tournament = turnuvaRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));

        if (tournament.getApprovalStatus() != ApplicationStatus.APPROVED || !tournament.getIsActive()) {
            throw new BaseException(ErrorCode.VAL_001, "Bu turnuva aktif değil veya henüz admin onayından geçmemiş.", HttpStatus.BAD_REQUEST, "");
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

        if (request.getClanId() == null) {
            throw new BaseException(ErrorCode.APP_002, "Tüm başvurular bir Clan (Takım) üzerinden yapılmalıdır. Clan ID zorunludur.", HttpStatus.BAD_REQUEST, "");
        }

        Clan clan = clanRepository.findById(request.getClanId())
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Clan bulunamadı", HttpStatus.NOT_FOUND, ""));

        if (!clan.getCategory().getId().equals(tournament.getCategory().getId())) {
            throw new BaseException(ErrorCode.APP_003, "Clan'ın kategorisi turnuva kategorisi ile uyuşmuyor", HttpStatus.BAD_REQUEST, "");
        }

        ClanMember member = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), applicantUserId)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_001, "Bu clan'ın üyesi değilsiniz", HttpStatus.FORBIDDEN, ""));

        if (member.getRole() != ClanMemberRole.OWNER && member.getRole() != ClanMemberRole.TEAM_CAPTAIN) {
            throw new BaseException(ErrorCode.APP_004, "Sadece clan sahibi veya takım kaptanı turnuvaya başvurabilir", HttpStatus.FORBIDDEN, "");
        }

        if (request.getSelectedClanMemberIds() == null || request.getSelectedClanMemberIds().isEmpty()) {
            throw new BaseException(ErrorCode.VAL_001, "Turnuvaya katılacak oyuncuları seçmelisiniz.", HttpStatus.BAD_REQUEST, "");
        }

        int rosterSize = request.getSelectedClanMemberIds().size();
        if (rosterSize < tournament.getMinTeamSize() || rosterSize > tournament.getMaxTeamSize()) {
            throw new BaseException(ErrorCode.VAL_001,
                    "Seçilen oyuncu sayısı turnuva kurallarına uymuyor. Gereken: Min " + tournament.getMinTeamSize() + ", Max " + tournament.getMaxTeamSize() + ". Seçilen: " + rosterSize,
                    HttpStatus.BAD_REQUEST, "");
        }

        for (Long userIdToSelect : request.getSelectedClanMemberIds()) {
            boolean isBusy = tournamentApplicationPlayerRepository.isPlayerBusyInDateRange(userIdToSelect, tournament.getStart_date(), tournament.getFinish_date());
            if (isBusy) {
                throw new PlayerAlreadyBusyException("Kullanıcı ID " + userIdToSelect + " tarihleri çakışan başka bir turnuvada asil kadroda yer alıyor.");
            }
        }

        TournamentApplication application = new TournamentApplication();
        application.setTournament(tournament);
        application.setUserId(applicantUserId);
        application.setStatus(TournamentApplicationStatus.PENDING);
        application.setIsCheckedIn(false);
        application.setClan(clan);

        List<TournamentApplicationPlayer> roster = new ArrayList<>();

        for (Long userIdToSelect : request.getSelectedClanMemberIds()) {
            ClanMember selectedMember = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), userIdToSelect)
                    .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Seçilen oyuncu klanınızda bulunamadı (Kullanıcı ID: " + userIdToSelect + ")", HttpStatus.NOT_FOUND, ""));

            TournamentApplicationPlayer player = new TournamentApplicationPlayer();
            player.setTournamentApplication(application);
            player.setClanMemberId(selectedMember.getId());
            player.setUserId(selectedMember.getUserId());
            roster.add(player);
        }

        application.setSelectedPlayers(roster);

        return tournamentApplicationRepository.save(application);
    }

    @Transactional
    public TournamentApplication updateApplicationStatus(Long applicationId, UpdateApplicationStatusRequestBody request) {
        TournamentApplication application = tournamentApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Başvuru bulunamadı", HttpStatus.NOT_FOUND, ""));

        checkOrgPermission(application.getTournament().getOrganizationId());
        
        TournamentApplicationStatus oldStatus = application.getStatus();

        application.setStatus(request.getStatus());
        application.setRejectionReason(xssSanitizer.sanitizeAndLimit(request.getRejectionReason(), 500));
        application.setReviewedAt(java.time.LocalDateTime.now());

        if (request.getStatus() == TournamentApplicationStatus.APPROVED && oldStatus != TournamentApplicationStatus.APPROVED) {
            Turnuva tournament = application.getTournament();
            
            if (tournament.getMaxParticipants() != null && tournament.getCurrentParticipantsCount() >= tournament.getMaxParticipants()) {
                tournament.setMaxParticipants(tournament.getMaxParticipants() + 1);
            }
            
            tournament.setCurrentParticipantsCount(tournament.getCurrentParticipantsCount() + 1);
            turnuvaRepository.save(tournament);
        } else if (oldStatus == TournamentApplicationStatus.APPROVED && request.getStatus() != TournamentApplicationStatus.APPROVED) {
            Turnuva tournament = application.getTournament();
            tournament.setCurrentParticipantsCount(tournament.getCurrentParticipantsCount() - 1);
            turnuvaRepository.save(tournament);
        }

        return tournamentApplicationRepository.save(application);
    }

    @Transactional
    public void finishTournamentAndDistributeRewards(Long tournamentId, RewardReportRequest reportRequest) {
        Turnuva turnuva = turnuvaRepository.findById(tournamentId)
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));

        checkOrgPermission(turnuva.getOrganizationId());

        if (!turnuva.getIsActive()) {
            throw new BaseException(ErrorCode.VAL_001, "Turnuva pasif durumda, ödül dağıtımı yapılamaz.", HttpStatus.BAD_REQUEST, "");
        }

        BigDecimal totalPrizePool = BigDecimal.valueOf(turnuva.getReward_amount() != null ? turnuva.getReward_amount() : 0.0);
        BigDecimal reportedTotal = BigDecimal.ZERO;

        for (RewardReportItem item : reportRequest.getRewards()) {
            reportedTotal = reportedTotal.add(item.getAmount());
        }

        if (reportedTotal.compareTo(totalPrizePool) != 0) {
            throw new BaseException(ErrorCode.VAL_001, "Raporlanan toplam ödül (" + reportedTotal + "), turnuvanın toplam ödül havuzuna (" + totalPrizePool + ") eşit olmalıdır.", HttpStatus.BAD_REQUEST, "");
        }

        for (RewardReportItem item : reportRequest.getRewards()) {
            TournamentApplication application = tournamentApplicationRepository.findById(item.getApplicationId())
                    .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Application bulunamadı: " + item.getApplicationId(), HttpStatus.NOT_FOUND, ""));

            List<TournamentApplicationPlayer> players = application.getSelectedPlayers();
            if (players == null || players.isEmpty()) continue;

            BigDecimal amountPerPlayer = item.getAmount().divide(BigDecimal.valueOf(players.size()), 4, RoundingMode.DOWN);

            for (TournamentApplicationPlayer player : players) {
                walletService.rewardMeydanCoin(
                        player.getUserId(), 
                        amountPerPlayer, 
                        "Turnuva Ödülü: " + turnuva.getTitle() + " (Takım: " + application.getClan().getName() + ")"
                );
            }
        }
        
        turnuva.setIsActive(false);
        turnuvaRepository.save(turnuva);
        logger.info("Turnuva başarıyla sonlandırıldı ve ödüller dağıtıldı. Turnuva ID: {}", tournamentId);
    }

    public List<TournamentApplication> getTournamentApplications(Long tournamentId) { 
        Turnuva turnuva = turnuvaRepository.findById(tournamentId)
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));
        
        checkOrgPermission(turnuva.getOrganizationId());
        return tournamentApplicationRepository.findByTournamentId(tournamentId); 
    }

    public List<TournamentApplication> getTournamentParticipants(Long tournamentId) {
        return tournamentApplicationRepository.findByTournamentIdAndStatus(tournamentId, TournamentApplicationStatus.APPROVED);
    }

    public List<TournamentApplication> getUserApplications(Long userId) { return tournamentApplicationRepository.findByUserId(userId); }

    @Transactional
    public TournamentApplication performCheckIn(Long tournamentId) {
        Long requesterUserId = getCurrentUserId();
        
        TournamentApplication application = tournamentApplicationRepository.findByTournamentIdAndUserId(tournamentId, requesterUserId)
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Bu turnuvaya ait bir başvurunuz bulunamadı.", HttpStatus.NOT_FOUND, ""));

        if (application.getStatus() != TournamentApplicationStatus.APPROVED) {
            throw new BaseException(ErrorCode.VAL_001, "Sadece onaylanmış başvurular yoklama (check-in) yapabilir.", HttpStatus.BAD_REQUEST, "");
        }

        Turnuva tournament = application.getTournament();
        
        // Timezone sorununu çözmek adına doğrudan UTC/Epoch Timestamp kıyaslaması (daha güvenli)
        long nowUtc = System.currentTimeMillis();
        long startTimeUtc = tournament.getStart_date().getTime();

        int checkInMinutes = 60;
        long checkInStartTimeUtc = startTimeUtc - (checkInMinutes * 60 * 1000L);

        if (nowUtc < checkInStartTimeUtc) {
            throw new BaseException(ErrorCode.VAL_001, "Yoklama süresi henüz başlamadı! Maça " + checkInMinutes + " dakika kala açılacaktır.", HttpStatus.BAD_REQUEST, "");
        }

        if (nowUtc > startTimeUtc) {
            throw new BaseException(ErrorCode.VAL_001, "Turnuva zaten başladı, check-in süresini kaçırdınız!", HttpStatus.BAD_REQUEST, "");
        }

        application.setIsCheckedIn(true);
        return tournamentApplicationRepository.save(application);
    }

    @Transactional
    public Turnuva updateTournamentDeadline(Long tournamentId, Date newDeadline) {
        Turnuva turnuva = turnuvaRepository.findById(tournamentId)
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_001, "Turnuva bulunamadı", HttpStatus.NOT_FOUND, ""));

        checkOrgPermission(turnuva.getOrganizationId());
        turnuva.setRegistrationDeadline(newDeadline);

        logger.info("Turnuva (ID: {}) kayıt süresi güncellendi.", tournamentId);
        return turnuvaRepository.save(turnuva);
    }

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
        application.setIsCheckedIn(false); 

        return tournamentApplicationRepository.save(application);
    }

    @Transactional
    public TournamentApplication respondToTournamentInvite(Long applicationId, RespondToTournamentInviteRequest request) {
        Long targetUserId = getCurrentUserId();
        
        TournamentApplication application = tournamentApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Davet bulunamadı", HttpStatus.NOT_FOUND, ""));

        Turnuva turnuva = application.getTournament();

        boolean isInvitee = application.getUserId().equals(targetUserId);
        boolean isOrgAdmin = false;
        
        if (!isInvitee) {
            isOrgAdmin = turnuva.getOrganizationId().equals(targetUserId) || 
                         organizationMembershipRepository.existsByOrganizationIdAndUserIdAndRoleIn(
                             turnuva.getOrganizationId(), targetUserId, Arrays.asList(OrganizationRole.OWNER, OrganizationRole.ADMIN));
        }

        if (!isInvitee && !isOrgAdmin) {
            throw new BaseException(ErrorCode.VAL_001, "Bu daveti yanıtlama yetkiniz yok.", HttpStatus.FORBIDDEN, "");
        }

        if (application.getStatus() != TournamentApplicationStatus.INVITED) {
            throw new BaseException(ErrorCode.VAL_001, "Geçerli bir davet bulunmuyor veya daha önceden yanıtlanmış.", HttpStatus.BAD_REQUEST, "");
        }

        if (request.getStatus() == TournamentApplicationStatus.APPROVED) {
            if (turnuva.getMaxParticipants() != null && turnuva.getCurrentParticipantsCount() >= turnuva.getMaxParticipants()) {
                throw new BaseException(ErrorCode.VAL_001, "Malesef siz daveti kabul edene kadar turnuva kapasitesi doldu. Yedek olarak kaydolmayı deneyebilirsiniz.", HttpStatus.BAD_REQUEST, "");
            }
            
            if (request.getClanId() == null) {
                throw new BaseException(ErrorCode.APP_002, "Daveti kabul etmek için takım (Clan) seçimi zorunludur.", HttpStatus.BAD_REQUEST, "");
            }

            Clan clan = clanRepository.findById(request.getClanId())
                    .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Clan bulunamadı", HttpStatus.NOT_FOUND, ""));

            if (!clan.getCategory().getId().equals(turnuva.getCategory().getId())) {
                throw new BaseException(ErrorCode.APP_003, "Clan'ın kategorisi turnuva kategorisi ile uyuşmuyor", HttpStatus.BAD_REQUEST, "");
            }

            ClanMember member = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), application.getUserId())
                    .orElseThrow(() -> new BaseException(ErrorCode.AUTH_001, "Bu clan'ın üyesi değilsiniz", HttpStatus.FORBIDDEN, ""));

            if (!isOrgAdmin && member.getRole() != ClanMemberRole.OWNER && member.getRole() != ClanMemberRole.TEAM_CAPTAIN) {
                throw new BaseException(ErrorCode.APP_004, "Sadece clan sahibi veya takım kaptanı daveti kabul edip takımı turnuvaya sokabilir", HttpStatus.FORBIDDEN, "");
            }

            application.setClan(clan);
            
            List<TournamentApplicationPlayer> roster = new ArrayList<>();

            if (request.getSelectedClanMemberIds() != null && !request.getSelectedClanMemberIds().isEmpty()) {
                int rosterSize = request.getSelectedClanMemberIds().size();
                if (rosterSize < turnuva.getMinTeamSize() || rosterSize > turnuva.getMaxTeamSize()) {
                    throw new BaseException(ErrorCode.VAL_001, 
                            "Seçilen oyuncu sayısı turnuva kurallarına uymuyor. Gereken: Min " + turnuva.getMinTeamSize() + ", Max " + turnuva.getMaxTeamSize() + ". Seçilen: " + rosterSize, 
                            HttpStatus.BAD_REQUEST, "");
                }

                for (Long userIdToSelect : request.getSelectedClanMemberIds()) {
                    boolean isBusy = tournamentApplicationPlayerRepository.isPlayerBusyInDateRange(userIdToSelect, turnuva.getStart_date(), turnuva.getFinish_date());
                    if (isBusy) {
                        throw new PlayerAlreadyBusyException("Kullanıcı ID " + userIdToSelect + " tarihleri çakışan başka bir turnuvada asil kadroda yer alıyor.");
                    }

                    ClanMember selectedMember = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), userIdToSelect)
                            .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Seçilen oyuncu klanınızda bulunamadı (Kullanıcı ID: " + userIdToSelect + ")", HttpStatus.NOT_FOUND, ""));

                    TournamentApplicationPlayer player = new TournamentApplicationPlayer();
                    player.setTournamentApplication(application);
                    player.setClanMemberId(selectedMember.getId());
                    player.setUserId(selectedMember.getUserId());
                    roster.add(player);
                }
            } else {
                throw new BaseException(ErrorCode.VAL_001, "Turnuvaya katılacak oyuncuları seçmelisiniz.", HttpStatus.BAD_REQUEST, "");
            }
            
            application.setSelectedPlayers(roster);
            application.setStatus(TournamentApplicationStatus.APPROVED);
            turnuva.setCurrentParticipantsCount(turnuva.getCurrentParticipantsCount() + 1);
            turnuvaRepository.save(turnuva);
            
        } else if (request.getStatus() == TournamentApplicationStatus.REJECTED || request.getStatus() == TournamentApplicationStatus.SUBSTITUTE) {
            application.setStatus(request.getStatus());
            if (request.getReason() != null) {
                application.setRejectionReason(xssSanitizer.sanitizeAndLimit(request.getReason(), 500));
            }
        } else {
            throw new BaseException(ErrorCode.VAL_001, "Geçersiz yanıt durumu. Yalnızca APPROVED, REJECTED veya SUBSTITUTE seçilebilir.", HttpStatus.BAD_REQUEST, "");
        }

        return tournamentApplicationRepository.save(application);
    }
}