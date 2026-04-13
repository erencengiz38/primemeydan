package com.meydan.meydan.service;

import com.meydan.meydan.exception.BaseException;
import com.meydan.meydan.exception.ErrorCode;
import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.models.enums.OrganizationRole;
import com.meydan.meydan.models.enums.TournamentApplicationStatus;
import com.meydan.meydan.repository.*;
import com.meydan.meydan.request.Turnuva.CreateStageRequest;
import com.meydan.meydan.request.Turnuva.CreateGroupRequest;
import com.meydan.meydan.request.Turnuva.GroupScoreListRequest;
import com.meydan.meydan.request.Turnuva.GroupScoreRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentMatchingService {

    private final TournamentStageRepository stageRepository;
    private final TournamentGroupRepository groupRepository;
    private final TournamentGroupAssignmentRepository assignmentRepository;
    private final TournamentApplicationRepository applicationRepository;
    private final TurnuvaRepository turnuvaRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final TournamentGroupScoreRepository scoreRepository; // Puanlama için eklendi

    // --- GÜVENLİK: BOLA KORUMASI ---
    private void checkOrganizationPermission(Long turnuvaId) {
        Long currentUserId = getCurrentUserId();

        Turnuva turnuva = turnuvaRepository.findById(turnuvaId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.TRN_001,
                        "Turnuva bulunamadı.",
                        HttpStatus.NOT_FOUND,
                        "Turnuva ID: " + turnuvaId
                ));

        Long organizationId = turnuva.getOrganizationId();
        
        boolean hasPermission = organizationMembershipRepository.existsByOrganizationIdAndUserIdAndRoleIn(
                organizationId,
                currentUserId,
                Arrays.asList(OrganizationRole.OWNER, OrganizationRole.ADMIN)
        );
        
        if (!hasPermission) {
            throw new BaseException(
                    ErrorCode.AUTH_001,
                    "Erişim Reddedildi: Bu işlem için organizasyon yetkilisi (OWNER veya ADMIN) olmanız gerekmektedir.",
                    HttpStatus.FORBIDDEN,
                    "Organization ID: " + organizationId + ", User ID: " + currentUserId
            );
        }
    }

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

    // Turnuvaya yeni bir aşama (Ön Eleme, Grup Aşaması vb.) ekleme
    @Transactional
    public TournamentStage createStage(Long turnuvaId, CreateStageRequest request) {
        checkOrganizationPermission(turnuvaId);

        Turnuva turnuva = turnuvaRepository.findById(turnuvaId).get(); // Yetki kontrolünde var olduğu doğrulandı

        TournamentStage stage = new TournamentStage();
        stage.setTurnuva(turnuva);
        stage.setName(request.getName());
        stage.setSequenceOrder(request.getSequenceOrder());
        stage.setStartDate(request.getStartDate());
        stage.setEndDate(request.getEndDate());
        return stageRepository.save(stage);
    }

    // Aşamaya yeni bir grup (A Grubu, B Grubu) ekleme
    @Transactional
    public TournamentGroup createGroup(Long stageId, CreateGroupRequest request) {
        TournamentStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.TRN_006,
                        "Aşama bulunamadı.",
                        HttpStatus.NOT_FOUND,
                        "Aşama ID: " + stageId
                ));

        checkOrganizationPermission(stage.getTurnuva().getId());

        TournamentGroup group = new TournamentGroup();
        group.setStage(stage);
        group.setName(request.getName());
        group.setMaxParticipants(request.getMaxParticipants());
        return groupRepository.save(group);
    }

    // AYRIŞTIR BUTONU MANTIĞI
    // Onaylanmış başvuruları bir aşamanın gruplarına rastgele dağıtır.
    @Transactional(isolation = Isolation.SERIALIZABLE) // Veri tutarlılığı için SERIALIZABLE izolasyon seviyesi
    public void distributeApplicationsToGroups(Long turnuvaId, Long stageId) {
        checkOrganizationPermission(turnuvaId);

        TournamentStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.TRN_006,
                        "Aşama bulunamadı.",
                        HttpStatus.NOT_FOUND,
                        "Aşama ID: " + stageId
                ));

        // Stage'in ilgili turnuvaya ait olup olmadığını doğrula
        if (!stage.getTurnuva().getId().equals(turnuvaId)) {
            throw new BaseException(
                    ErrorCode.TRN_007,
                    "Belirtilen aşama bu turnuvaya ait değil.",
                    HttpStatus.BAD_REQUEST,
                    "Turnuva ID: " + turnuvaId + ", Aşama ID: " + stageId
            );
        }

        List<TournamentGroup> groups = groupRepository.findByStageId(stageId);
        if (groups.isEmpty()) {
            throw new BaseException(
                    ErrorCode.TRN_008,
                    "Aşamaya ait grup (A, B vs.) bulunamadı. Lütfen önce grup oluşturun.",
                    HttpStatus.BAD_REQUEST,
                    "Aşama ID: " + stageId
            );
        }

        // Mevcut atamaları temizle (Yeniden ayrıştır yapılıyorsa)
        // Tüm grupların atamalarını silmek için döngü kullanabiliriz
        for (TournamentGroup group : groups) {
            assignmentRepository.deleteByGroupId(group.getId());
        }
        assignmentRepository.flush(); // Silme işlemini bekletmeden DB'ye yaz (Hata çıkmasını önler)

        // YENİ KURAL 1: Check-in Filtresi
        // Turnuvaya ait sadece KABUL EDİLMİŞ (APPROVED) VE YOKLAMA YAPMIŞ (isCheckedIn = true) başvuruları getir
        List<TournamentApplication> validApplications = applicationRepository
                .findByTournamentIdAndStatusAndIsCheckedInTrue(turnuvaId, TournamentApplicationStatus.APPROVED);

        if (validApplications.isEmpty()) {
            throw new BaseException(
                    ErrorCode.TRN_009,
                    "Dağıtılacak geçerli (Onaylı ve Yoklama Yapmış) başvuru bulunamadı.",
                    HttpStatus.NOT_FOUND,
                    "Turnuva ID: " + turnuvaId
            );
        }

        // Rastgelelik kat (Ayrıştırma)
        List<TournamentApplication> shuffledApplications = new ArrayList<>(validApplications);
        Collections.shuffle(shuffledApplications);

        // Toplu Atama Listesi Hazırlama
        List<TournamentGroupAssignment> newAssignments = new ArrayList<>();
        int currentGroupIndex = 0;
        
        // YENİ KURAL 4: Grup Kapasite Kontrolü
        // Grupların mevcut doluluk durumlarını takip etmek için bir array oluşturalım
        int[] currentGroupCounts = new int[groups.size()];

        for (TournamentApplication application : shuffledApplications) {
            boolean assigned = false;
            int startGroupIndex = currentGroupIndex;

            // Kapasitesi olan bir grup bulana kadar döngü
            do {
                TournamentGroup currentGroup = groups.get(currentGroupIndex);
                
                if (currentGroupCounts[currentGroupIndex] < currentGroup.getMaxParticipants()) {
                    // Gruba yerleştir
                    TournamentGroupAssignment assignment = new TournamentGroupAssignment();
                    assignment.setGroup(currentGroup);
                    assignment.setApplication(application);
                    newAssignments.add(assignment);
                    
                    currentGroupCounts[currentGroupIndex]++;
                    assigned = true;
                    
                    // Sıradaki gruba geç (Round-robin)
                    currentGroupIndex = (currentGroupIndex + 1) % groups.size();
                    break; // İç döngüden çık
                }
                
                // Grup doluysa sonraki gruba geçip tekrar dene
                currentGroupIndex = (currentGroupIndex + 1) % groups.size();
            } while (currentGroupIndex != startGroupIndex); // Tüm gruplara bakıldıysa çık

            // Tüm gruplar doluysa takımı YEDEK (SUBSTITUTE) yap
            if (!assigned) {
                application.setStatus(TournamentApplicationStatus.SUBSTITUTE);
                applicationRepository.save(application); // SUBSTITUTE olarak güncelle
            }
        }

        // Hazırlanan listeyi tek seferde veritabanına yaz
        assignmentRepository.saveAll(newAssignments);
    }

    // --- LOBİ/GRUP PUANLAMA VE ELEME SİSTEMİ ---
    
    @Transactional
    public void reportGroupScores(Long turnuvaId, Long groupId, GroupScoreListRequest request) {
        checkOrganizationPermission(turnuvaId);
        Long reporterId = getCurrentUserId();
        
        TournamentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BaseException(ErrorCode.TRN_006, "Grup bulunamadı.", HttpStatus.NOT_FOUND, "Grup ID: " + groupId));
                
        if (!group.getStage().getTurnuva().getId().equals(turnuvaId)) {
            throw new BaseException(ErrorCode.VAL_001, "Grup bu turnuvaya ait değil.", HttpStatus.BAD_REQUEST, "");
        }
        
        List<TournamentGroupScore> newScores = new ArrayList<>();
        
        for (GroupScoreRequest scoreReq : request.getScores()) {
            TournamentApplication app = applicationRepository.findById(scoreReq.getApplicationId())
                    .orElseThrow(() -> new BaseException(ErrorCode.VAL_001, "Başvuru bulunamadı", HttpStatus.NOT_FOUND, "App ID: " + scoreReq.getApplicationId()));
                    
            TournamentGroupScore score = scoreRepository.findByGroupIdAndApplicationId(groupId, app.getId())
                    .orElse(new TournamentGroupScore());
                    
            score.setGroup(group);
            score.setApplication(app);
            score.setScore(scoreReq.getScore());
            if (scoreReq.getPlacement() != null) score.setPlacement(scoreReq.getPlacement());
            if (scoreReq.getIsAdvanced() != null) score.setIsAdvanced(scoreReq.getIsAdvanced());
            
            score.setReportedById(reporterId);
            score.setUpdatedAt(java.time.LocalDateTime.now());
            
            newScores.add(score);
        }
        
        scoreRepository.saveAll(newScores);
    }
    
    public List<TournamentGroupScore> getGroupScores(Long groupId) {
        return scoreRepository.findByGroupIdOrderByScoreDesc(groupId);
    }
}
