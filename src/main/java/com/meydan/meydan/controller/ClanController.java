package com.meydan.meydan.controller;

import com.meydan.meydan.config.CurrentUserId;
import com.meydan.meydan.dto.ApiResponse;
import com.meydan.meydan.dto.response.ClanInvitationResponseDTO;
import com.meydan.meydan.dto.response.ClanMemberResponseDTO;
import com.meydan.meydan.dto.response.ClanResponseDTO;
import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.repository.UserRepository;
import com.meydan.meydan.request.Clan.AddClanRequestBody;
import com.meydan.meydan.request.Clan.InviteToClanRequest;
import com.meydan.meydan.request.Clan.RespondToInvitationRequest;
import com.meydan.meydan.request.Clan.UpdateClanMemberRoleRequestBody;
import com.meydan.meydan.service.ClanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clan")
@Tag(name = "Clan", description = "Clan (Takım) API endpoint'leri")
@RequiredArgsConstructor
public class ClanController {

    private final ClanService clanService;
    private final UserRepository userRepository; // Kullanıcı detayları için eklendi
    private final ModelMapper modelMapper;

    // --- Helper Methods for DTO Mapping ---
    private ClanResponseDTO mapToClanDTO(Clan clan) {
        ClanResponseDTO dto = modelMapper.map(clan, ClanResponseDTO.class);
        if (clan.getCategory() != null) {
            dto.setCategoryName(clan.getCategory().getName());
        }
        return dto;
    }

    private ClanMemberResponseDTO mapToClanMemberDTO(ClanMember member) {
        ClanMemberResponseDTO dto = modelMapper.map(member, ClanMemberResponseDTO.class);
        if (member.getClan() != null) {
            dto.setClanName(member.getClan().getName());
        }
        return dto;
    }

    private ClanInvitationResponseDTO mapToClanInvitationDTO(ClanInvitation invitation) {
        ClanInvitationResponseDTO dto = modelMapper.map(invitation, ClanInvitationResponseDTO.class);
        if (invitation.getClan() != null) {
            dto.setClanName(invitation.getClan().getName());
        }

        // Başvuranın / Davet edilenin bilgilerini UserRepository'den çekip DTO'ya ekle
        if (invitation.getUserId() != null) {
            Optional<User> userOpt = userRepository.findById(invitation.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                dto.setUserName(user.getDisplay_name());
                dto.setUserTag(user.getTag());
            }
        }
        return dto;
    }

    // --- Clan Read Endpoints ---
    @GetMapping("/list")
    @Operation(summary = "Tüm aktif clanları listele")
    public ResponseEntity<ApiResponse<List<ClanResponseDTO>>> getAllClans() {
        List<ClanResponseDTO> dtoList = clanService.getAllClans().stream()
                .map(this::mapToClanDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Clanlar başarıyla getirildi", dtoList));
    }

    @GetMapping("/list/paginated")
    @Operation(summary = "Clanları sayfalı olarak listele")
    public ResponseEntity<ApiResponse<Page<ClanResponseDTO>>> getAllClansWithPagination(Pageable pageable) {
        Page<ClanResponseDTO> dtoPage = clanService.getAllClansWithPagination(pageable)
                .map(this::mapToClanDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Clanlar başarıyla getirildi", dtoPage));
    }
    
    @GetMapping("/{clanId}")
    @Operation(summary = "ID ile clan detaylarını getir")
    public ResponseEntity<ApiResponse<ClanResponseDTO>> getClanById(@PathVariable Long clanId) {
        Clan clan = clanService.getClanById(clanId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Clan detayları getirildi", mapToClanDTO(clan)));
    }

    // --- Clan Create Endpoint ---
    @PostMapping("/create")
    @Operation(summary = "Yeni clan oluştur")
    public ResponseEntity<ApiResponse<ClanResponseDTO>> createClan(@Valid @RequestBody AddClanRequestBody request) {
        Clan clan = clanService.createClan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Clan başarıyla oluşturuldu", mapToClanDTO(clan)));
    }

    // --- Invitation and Application Endpoints ---

    @PostMapping("/{clanId}/invite")
    @Operation(summary = "Kullanıcıyı clana davet et (Owner/Manager)")
    public ResponseEntity<ApiResponse<ClanInvitationResponseDTO>> inviteToClan(@PathVariable Long clanId, @RequestBody InviteToClanRequest request) {
        ClanInvitation invitation = clanService.inviteToClan(clanId, request.getUserIdToInvite());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Davet başarıyla gönderildi", mapToClanInvitationDTO(invitation)));
    }

    @PostMapping("/{clanId}/apply")
    @Operation(summary = "Clana katılmak için başvur")
    public ResponseEntity<ApiResponse<ClanInvitationResponseDTO>> applyToClan(@PathVariable Long clanId) {
        ClanInvitation application = clanService.applyToClan(clanId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Başvurunuz başarıyla alındı", mapToClanInvitationDTO(application)));
    }

    @PostMapping("/invitations/{invitationId}/respond")
    @Operation(summary = "Sana gelen bir daveti yanıtla (Kabul/Red)")
    public ResponseEntity<ApiResponse<ClanInvitationResponseDTO>> respondToInvitation(@PathVariable Long invitationId, @Valid @RequestBody RespondToInvitationRequest request) {
        ClanInvitation invitation = clanService.respondToInvitation(invitationId, request.getAccept(), request.getReason());
        String message = request.getAccept() ? "Davet kabul edildi." : "Davet reddedildi.";
        return ResponseEntity.ok(new ApiResponse<>(true, message, mapToClanInvitationDTO(invitation)));
    }

    @PostMapping("/applications/{applicationId}/respond")
    @Operation(summary = "Bir başvuruyu yanıtla (Kabul/Red) (Owner/Manager)")
    public ResponseEntity<ApiResponse<ClanInvitationResponseDTO>> respondToApplication(@PathVariable Long applicationId, @Valid @RequestBody RespondToInvitationRequest request) {
        ClanInvitation application = clanService.respondToApplication(applicationId, request.getAccept(), request.getReason());
        String message = request.getAccept() ? "Başvuru kabul edildi." : "Başvuru reddedildi.";
        return ResponseEntity.ok(new ApiResponse<>(true, message, mapToClanInvitationDTO(application)));
    }

    @PostMapping("/invitations/{invitationId}/cancel")
    @Operation(summary = "Bekleyen bir daveti/başvuruyu iptal et")
    public ResponseEntity<ApiResponse<ClanInvitationResponseDTO>> cancelInvitation(@PathVariable Long invitationId) {
        ClanInvitation invitation = clanService.cancelInvitation(invitationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "İşlem başarıyla iptal edildi.", mapToClanInvitationDTO(invitation)));
    }
    
    @GetMapping("/invitations/pending/my")
    @Operation(summary = "Sana gelen bekleyen davetleri listele")
    public ResponseEntity<ApiResponse<List<ClanInvitationResponseDTO>>> getMyPendingInvitations(@CurrentUserId Long userId) {
        List<ClanInvitationResponseDTO> dtoList = clanService.getPendingInvitationsForUser(userId).stream()
                .map(this::mapToClanInvitationDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Bekleyen davetleriniz getirildi.", dtoList));
    }

    @GetMapping("/{clanId}/invitations/pending")
    @Operation(summary = "Bir clanın bekleyen davetlerini/başvurularını listele (Owner/Manager)")
    public ResponseEntity<ApiResponse<List<ClanInvitationResponseDTO>>> getClanPendingInvitations(@PathVariable Long clanId) {
        List<ClanInvitationResponseDTO> dtoList = clanService.getPendingInvitationsForClan(clanId).stream()
                .map(this::mapToClanInvitationDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Clanın bekleyen işlemleri getirildi.", dtoList));
    }

    @GetMapping("/{clanId}/applications")
    @Operation(summary = "Bir clana gelen tüm başvuruları listele (Owner/Manager)")
    public ResponseEntity<ApiResponse<List<ClanInvitationResponseDTO>>> getClanApplications(@PathVariable Long clanId) {
        List<ClanInvitationResponseDTO> dtoList = clanService.getClanApplications(clanId).stream()
                .map(this::mapToClanInvitationDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Clana gelen başvurular getirildi.", dtoList));
    }

    // --- Member Management Endpoints ---

    @GetMapping("/{clanId}/members")
    @Operation(summary = "Clan üyelerini listele")
    public ResponseEntity<ApiResponse<List<ClanMemberResponseDTO>>> getClanMembers(@PathVariable Long clanId) {
        List<ClanMemberResponseDTO> dtoList = clanService.getClanMembers(clanId).stream()
                .map(this::mapToClanMemberDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Clan üyeleri başarıyla getirildi", dtoList));
    }

    @DeleteMapping("/member/{clanMemberId}")
    @Operation(summary = "Clan'dan üye çıkar (Owner/Manager)")
    public ResponseEntity<ApiResponse<ClanMemberResponseDTO>> kickMember(@PathVariable Long clanMemberId) {
        ClanMember member = clanService.kickMember(clanMemberId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Üye başarıyla clan'dan çıkarıldı", mapToClanMemberDTO(member)));
    }

    @PutMapping("/member/role")
    @Operation(summary = "Üye rolünü güncelle (Owner)")
    public ResponseEntity<ApiResponse<ClanMemberResponseDTO>> updateMemberRole(@Valid @RequestBody UpdateClanMemberRoleRequestBody request) {
        ClanMember member = clanService.updateMemberRole(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Üye rolü başarıyla güncellendi", mapToClanMemberDTO(member)));
    }
    
    @DeleteMapping("/{clanId}/leave")
    @Operation(summary = "Klandan Ayrıl", description = "Sahipsiz klan koruması ile klandan ayrılma")
    public ResponseEntity<ApiResponse<Void>> leaveClan(@PathVariable Long clanId) {
        clanService.leaveClan(clanId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Klandan başarıyla ayrıldınız.", null));
    }

    @GetMapping("/my")
    @Operation(summary = "Kendi clanlarımı listele")
    public ResponseEntity<ApiResponse<List<ClanMemberResponseDTO>>> getMyClans(@CurrentUserId Long userId) {
        List<ClanMemberResponseDTO> dtoList = clanService.getUserClans(userId).stream()
                .map(this::mapToClanMemberDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Kendi clanlarınız başarıyla getirildi", dtoList));
    }
}
