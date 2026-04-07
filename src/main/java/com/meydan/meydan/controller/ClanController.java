package com.meydan.meydan.controller;

import com.meydan.meydan.dto.ApiResponse;
import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.request.Auth.Clan.AddClanRequestBody;
import com.meydan.meydan.request.Auth.Clan.InviteToClanRequest;
import com.meydan.meydan.request.Auth.Clan.RespondToInvitationRequest;
import com.meydan.meydan.request.Auth.Clan.UpdateClanMemberRoleRequestBody;
import com.meydan.meydan.service.ClanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clan")
@Tag(name = "Clan", description = "Clan (Takım) API endpoint'leri")
@RequiredArgsConstructor
public class ClanController {

    private final ClanService clanService;

    private Long getUserIdFromAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Kullanıcı kimlik doğrulaması gerekli");
        }
        // Assuming the principal is the User object or at least contains the ID.
        // This might need adjustment based on your UserDetails implementation.
        // For this example, let's assume it's a User object.
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }

    // --- Clan Read Endpoints ---
    @GetMapping("/list")
    @Operation(summary = "Tüm aktif clanları listele")
    public ResponseEntity<ApiResponse<List<Clan>>> getAllClans() {
        List<Clan> clans = clanService.getAllClans();
        return ResponseEntity.ok(new ApiResponse<>(true, "Clanlar başarıyla getirildi", clans));
    }

    @GetMapping("/list/paginated")
    @Operation(summary = "Clanları sayfalı olarak listele")
    public ResponseEntity<ApiResponse<Page<Clan>>> getAllClansWithPagination(Pageable pageable) {
        Page<Clan> clans = clanService.getAllClansWithPagination(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Clanlar başarıyla getirildi", clans));
    }
    
    @GetMapping("/{clanId}")
    @Operation(summary = "ID ile clan detaylarını getir")
    public ResponseEntity<ApiResponse<Clan>> getClanById(@PathVariable Long clanId) {
        Clan clan = clanService.getClanById(clanId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Clan detayları getirildi", clan));
    }

    // --- Clan Create Endpoint ---
    @PostMapping("/create")
    @Operation(summary = "Yeni clan oluştur")
    public ResponseEntity<ApiResponse<Clan>> createClan(@Valid @RequestBody AddClanRequestBody request) {
        Long creatorUserId = getUserIdFromAuthentication();
        Clan clan = clanService.createClan(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Clan başarıyla oluşturuldu", clan));
    }

    // --- Invitation and Application Endpoints ---

    @PostMapping("/{clanId}/invite")
    @Operation(summary = "Kullanıcıyı clana davet et (Owner/Manager)")
    public ResponseEntity<ApiResponse<ClanInvitation>> inviteToClan(@PathVariable Long clanId, @RequestBody InviteToClanRequest request) {
        Long requesterUserId = getUserIdFromAuthentication();
        ClanInvitation invitation = clanService.inviteToClan(clanId, request.getUserIdToInvite(), requesterUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Davet başarıyla gönderildi", invitation));
    }

    @PostMapping("/{clanId}/apply")
    @Operation(summary = "Clana katılmak için başvur")
    public ResponseEntity<ApiResponse<ClanInvitation>> applyToClan(@PathVariable Long clanId) {
        Long applicantUserId = getUserIdFromAuthentication();
        ClanInvitation application = clanService.applyToClan(clanId, applicantUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Başvurunuz başarıyla alındı", application));
    }

    @PostMapping("/invitations/{invitationId}/respond")
    @Operation(summary = "Sana gelen bir daveti yanıtla (Kabul/Red)")
    public ResponseEntity<ApiResponse<ClanInvitation>> respondToInvitation(@PathVariable Long invitationId, @Valid @RequestBody RespondToInvitationRequest request) {
        Long respondingUserId = getUserIdFromAuthentication();
        ClanInvitation invitation = clanService.respondToInvitation(invitationId, respondingUserId, request.getAccept(), request.getReason());
        String message = request.getAccept() ? "Davet kabul edildi." : "Davet reddedildi.";
        return ResponseEntity.ok(new ApiResponse<>(true, message, invitation));
    }

    @PostMapping("/applications/{applicationId}/respond")
    @Operation(summary = "Bir başvuruyu yanıtla (Kabul/Red) (Owner/Manager)")
    public ResponseEntity<ApiResponse<ClanInvitation>> respondToApplication(@PathVariable Long applicationId, @Valid @RequestBody RespondToInvitationRequest request) {
        Long requesterUserId = getUserIdFromAuthentication();
        ClanInvitation application = clanService.respondToApplication(applicationId, requesterUserId, request.getAccept(), request.getReason());
        String message = request.getAccept() ? "Başvuru kabul edildi." : "Başvuru reddedildi.";
        return ResponseEntity.ok(new ApiResponse<>(true, message, application));
    }

    @PostMapping("/invitations/{invitationId}/cancel")
    @Operation(summary = "Bekleyen bir daveti/başvuruyu iptal et")
    public ResponseEntity<ApiResponse<ClanInvitation>> cancelInvitation(@PathVariable Long invitationId) {
        Long requesterUserId = getUserIdFromAuthentication();
        ClanInvitation invitation = clanService.cancelInvitation(invitationId, requesterUserId);
        return ResponseEntity.ok(new ApiResponse<>(true, "İşlem başarıyla iptal edildi.", invitation));
    }
    
    @GetMapping("/invitations/pending/my")
    @Operation(summary = "Sana gelen bekleyen davetleri listele")
    public ResponseEntity<ApiResponse<List<ClanInvitation>>> getMyPendingInvitations() {
        Long userId = getUserIdFromAuthentication();
        List<ClanInvitation> invitations = clanService.getPendingInvitationsForUser(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Bekleyen davetleriniz getirildi.", invitations));
    }

    @GetMapping("/{clanId}/invitations/pending")
    @Operation(summary = "Bir clanın bekleyen davetlerini/başvurularını listele (Owner/Manager)")
    public ResponseEntity<ApiResponse<List<ClanInvitation>>> getClanPendingInvitations(@PathVariable Long clanId) {
        Long requesterUserId = getUserIdFromAuthentication();
        List<ClanInvitation> invitations = clanService.getPendingInvitationsForClan(clanId, requesterUserId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Clanın bekleyen işlemleri getirildi.", invitations));
    }

    // --- Member Management Endpoints ---

    @GetMapping("/{clanId}/members")
    @Operation(summary = "Clan üyelerini listele")
    public ResponseEntity<ApiResponse<List<ClanMember>>> getClanMembers(@PathVariable Long clanId) {
        List<ClanMember> members = clanService.getClanMembers(clanId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Clan üyeleri başarıyla getirildi", members));
    }

    @DeleteMapping("/member/{clanMemberId}")
    @Operation(summary = "Clan'dan üye çıkar (Owner/Manager)")
    public ResponseEntity<ApiResponse<ClanMember>> kickMember(@PathVariable Long clanMemberId) {
        Long requesterUserId = getUserIdFromAuthentication();
        ClanMember member = clanService.kickMember(clanMemberId, requesterUserId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Üye başarıyla clan'dan çıkarıldı", member));
    }

    @PutMapping("/member/role")
    @Operation(summary = "Üye rolünü güncelle (Owner)")
    public ResponseEntity<ApiResponse<ClanMember>> updateMemberRole(@Valid @RequestBody UpdateClanMemberRoleRequestBody request) {
        Long requesterUserId = getUserIdFromAuthentication();
        ClanMember member = clanService.updateMemberRole(request, requesterUserId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Üye rolü başarıyla güncellendi", member));
    }

    @GetMapping("/my")
    @Operation(summary = "Kendi clanlarımı listele")
    public ResponseEntity<ApiResponse<List<ClanMember>>> getMyClans() {
        Long userId = getUserIdFromAuthentication();
        List<ClanMember> clans = clanService.getUserClans(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Kendi clanlarınız başarıyla getirildi", clans));
    }
}
