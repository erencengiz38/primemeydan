package com.meydan.meydan.controller;

import com.meydan.meydan.dto.ApiResponse;
import com.meydan.meydan.dto.response.TournamentApplicationResponseDTO;
import com.meydan.meydan.dto.response.TurnuvaResponseDTO;
import com.meydan.meydan.models.entities.TournamentApplication;
import com.meydan.meydan.models.entities.Turnuva;
import com.meydan.meydan.repository.OrganizationRepository;
import com.meydan.meydan.request.AdminReviewRequest;
import com.meydan.meydan.request.Turnuva.AddTurnuvaRequestBody;
import com.meydan.meydan.request.Turnuva.ApplyToTournamentRequestBody;
import com.meydan.meydan.request.Turnuva.UpdateApplicationStatusRequestBody;
import com.meydan.meydan.request.Turnuva.UpdateTurnuvaRequestBody;
import com.meydan.meydan.request.Turnuva.RespondToTournamentInviteRequest;
import com.meydan.meydan.request.Turnuva.RewardReportRequest;
import com.meydan.meydan.service.TurnuvaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/turnuva")
@Tag(name = "Turnuva", description = "Turnuva API endpoint'leri")
@RequiredArgsConstructor
public class TurnuvaController {

    private final TurnuvaService turnuvaService;
    private final ModelMapper modelMapper;
    private final OrganizationRepository organizationRepository;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getName());
    }

    private TurnuvaResponseDTO mapToTurnuvaDTO(Turnuva turnuva) {
        TurnuvaResponseDTO dto = modelMapper.map(turnuva, TurnuvaResponseDTO.class);

        if (turnuva.getCategory() != null) {
            dto.setCategoryId(turnuva.getCategory().getId());
            dto.setCategoryName(turnuva.getCategory().getName());
        }

        if (turnuva.getOrganizationId() != null) {
            organizationRepository.findById(turnuva.getOrganizationId())
                    .ifPresent(org -> dto.setOrganizationLogoUrl(org.getLogoUrl()));
        }

        if (dto.getEntryFee() == null) {
            dto.setEntryFee(0.0);
        }

        return dto;
    }

    private TournamentApplicationResponseDTO mapToApplicationDTO(TournamentApplication app) {
        TournamentApplicationResponseDTO dto = modelMapper.map(app, TournamentApplicationResponseDTO.class);

        if (app.getTournament() != null) {
            dto.setTournamentId(app.getTournament().getId());
            dto.setTournamentTitle(app.getTournament().getTitle());
        }

        if (app.getClan() != null) {
            dto.setClanId(app.getClan().getId());
            dto.setClanName(app.getClan().getName());
        }

        return dto;
    }

    @PostMapping("/{organizationId}/create")
    @Operation(summary = "Yeni turnuva oluştur", description = "Oluşturulan turnuva otomatik olarak ONAYLANIR (APPROVED) ve yayına alınır.")
    public ResponseEntity<ApiResponse<TurnuvaResponseDTO>> createTurnuva(
            @PathVariable Long organizationId,
            @Valid @RequestBody AddTurnuvaRequestBody request) {

        Turnuva turnuva = turnuvaService.createTurnuva(request, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Turnuva başarıyla oluşturuldu ve yayına alındı.", mapToTurnuvaDTO(turnuva)));
    }

    @PutMapping("/{organizationId}/update")
    @Operation(summary = "Turnuvayı güncelle")
    public ResponseEntity<ApiResponse<TurnuvaResponseDTO>> updateTurnuva(
            @PathVariable Long organizationId,
            @Valid @RequestBody UpdateTurnuvaRequestBody request) {

        Turnuva updatedTurnuva = turnuvaService.updateTurnuva(request, organizationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva güncellendi", mapToTurnuvaDTO(updatedTurnuva)));
    }

    @DeleteMapping("/{id}/{organizationId}")
    @Operation(summary = "Turnuvayı sil", description = "Soft delete işlemi")
    public ResponseEntity<ApiResponse<TurnuvaResponseDTO>> deleteTurnuva(
            @PathVariable Long id,
            @PathVariable Long organizationId) {

        Turnuva deletedTurnuva = turnuvaService.deleteTurnuva(id, organizationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva silindi", mapToTurnuvaDTO(deletedTurnuva)));
    }

    @PostMapping("/{id}/{organizationId}/restore")
    @Operation(summary = "Turnuvayı geri yükle", description = "Geri yüklenen turnuva otomatik olarak ONAYLANIR (APPROVED) ve yayına alınır.")
    public ResponseEntity<ApiResponse<TurnuvaResponseDTO>> restoreTurnuva(
            @PathVariable Long id,
            @PathVariable Long organizationId) {

        Turnuva restoredTurnuva = turnuvaService.restoreTurnuva(id, organizationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva geri yüklendi ve yayına alındı.", mapToTurnuvaDTO(restoredTurnuva)));
    }

    @DeleteMapping("/{id}/{organizationId}/permanent")
    @Operation(summary = "Turnuvayı kalıcı olarak sil")
    public ResponseEntity<ApiResponse<TurnuvaResponseDTO>> permanentlyDeleteTurnuva(
            @PathVariable Long id,
            @PathVariable Long organizationId) {

        Turnuva deletedTurnuva = turnuvaService.permanentlyDeleteTurnuva(id, organizationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva kalıcı olarak silindi", mapToTurnuvaDTO(deletedTurnuva)));
    }

    @GetMapping("/list")
    @Operation(summary = "Tüm ONAYLANMIŞ turnuvaları listeler")
    public ResponseEntity<ApiResponse<List<TurnuvaResponseDTO>>> getAllTurnuvas() {
        List<TurnuvaResponseDTO> dtoList = turnuvaService.getAllTurnuvas().stream()
                .map(this::mapToTurnuvaDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuvalar getirildi", dtoList));
    }

    @GetMapping("/list/paginated")
    @Operation(summary = "ONAYLANMIŞ turnuvaları sayfalı listeler")
    public ResponseEntity<ApiResponse<Page<TurnuvaResponseDTO>>> getAllTurnuvasWithPagination(Pageable pageable) {
        Page<TurnuvaResponseDTO> dtoPage = turnuvaService.getAllTurnuvasWithPagination(pageable)
                .map(this::mapToTurnuvaDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuvalar getirildi", dtoPage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Turnuva detaylarını getir (ID ile)")
    public ResponseEntity<ApiResponse<TurnuvaResponseDTO>> getTurnuvaById(@PathVariable Long id) {
        Turnuva turnuva = turnuvaService.getTurnuvaById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva detayları getirildi", mapToTurnuvaDTO(turnuva)));
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Organizasyon turnuvalarını listele (O organizasyona ait hepsi)")
    public ResponseEntity<ApiResponse<List<TurnuvaResponseDTO>>> getTurnuvasByOrganization(@PathVariable Long organizationId) {
        List<TurnuvaResponseDTO> dtoList = turnuvaService.getTurnuvasByOrganizationId(organizationId).stream()
                .map(this::mapToTurnuvaDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuvalar getirildi", dtoList));
    }

    @GetMapping("/organization/{organizationId}/paginated")
    @Operation(summary = "Organizasyon turnuvalarını sayfalı listele")
    public ResponseEntity<ApiResponse<Page<TurnuvaResponseDTO>>> getTurnuvasByOrganizationWithPagination(@PathVariable Long organizationId, Pageable pageable) {
        Page<TurnuvaResponseDTO> dtoPage = turnuvaService.getTurnuvasByOrganizationIdWithPagination(organizationId, pageable)
                .map(this::mapToTurnuvaDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuvalar getirildi", dtoPage));
    }

    @GetMapping("/my")
    @Operation(summary = "Kendi organizasyonumun turnuvalarını listele")
    public ResponseEntity<ApiResponse<List<TurnuvaResponseDTO>>> getMyTurnuvas() {
        Long organizationId = getCurrentUserId();
        List<TurnuvaResponseDTO> dtoList = turnuvaService.getTurnuvasByOrganizationId(organizationId).stream()
                .map(this::mapToTurnuvaDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Kendi turnuvalarınız getirildi", dtoList));
    }

    @GetMapping("/my/paginated")
    @Operation(summary = "Kendi organizasyonumun turnuvalarını sayfalı listele")
    public ResponseEntity<ApiResponse<Page<TurnuvaResponseDTO>>> getMyTurnuvasWithPagination(Pageable pageable) {
        Long organizationId = getCurrentUserId();
        Page<TurnuvaResponseDTO> dtoPage = turnuvaService.getTurnuvasByOrganizationIdWithPagination(organizationId, pageable)
                .map(this::mapToTurnuvaDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Kendi turnuvalarınız getirildi", dtoPage));
    }

    @GetMapping("/admin/pending")
    @Operation(summary = "Onay bekleyen turnuvaları listele (Sadece Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TurnuvaResponseDTO>>> getPendingTurnuvas() {
        List<TurnuvaResponseDTO> dtoList = turnuvaService.getPendingTurnuvas().stream()
                .map(this::mapToTurnuvaDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Onay bekleyen turnuvalar getirildi", dtoList));
    }

    @PostMapping("/admin/{turnuvaId}/approve")
    @Operation(summary = "Turnuvayı onayla (Sadece Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TurnuvaResponseDTO>> approveTurnuva(
            @PathVariable Long turnuvaId,
            @RequestBody(required = false) AdminReviewRequest request) {

        String notes = request != null ? request.getAdminNotes() : null;
        Turnuva turnuva = turnuvaService.approveTurnuva(turnuvaId, notes);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva onaylandı ve yayına alındı", mapToTurnuvaDTO(turnuva)));
    }

    @PostMapping("/admin/{turnuvaId}/reject")
    @Operation(summary = "Turnuvayı reddet (Sadece Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TurnuvaResponseDTO>> rejectTurnuva(
            @PathVariable Long turnuvaId,
            @RequestBody AdminReviewRequest request) {

        Turnuva turnuva = turnuvaService.rejectTurnuva(turnuvaId, request.getAdminNotes());
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva reddedildi", mapToTurnuvaDTO(turnuva)));
    }

    @PostMapping("/apply")
    @Operation(summary = "Turnuvaya başvur")
    public ResponseEntity<ApiResponse<TournamentApplicationResponseDTO>> applyToTournament(
            @Valid @RequestBody ApplyToTournamentRequestBody request) {

        TournamentApplication application = turnuvaService.applyToTournament(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Başvuru alındı", mapToApplicationDTO(application)));
    }

    @PutMapping("/application/{applicationId}/status")
    @Operation(summary = "Başvuru durumunu güncelle")
    public ResponseEntity<ApiResponse<TournamentApplicationResponseDTO>> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequestBody request) {

        TournamentApplication application = turnuvaService.updateApplicationStatus(applicationId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Durum güncellendi", mapToApplicationDTO(application)));
    }

    @GetMapping("/{tournamentId}/applications")
    @Operation(summary = "Turnuva başvurularını listele (Yetkili)")
    public ResponseEntity<ApiResponse<List<TournamentApplicationResponseDTO>>> getTournamentApplications(@PathVariable Long tournamentId) {
        List<TournamentApplicationResponseDTO> dtoList = turnuvaService.getTournamentApplications(tournamentId).stream()
                .map(this::mapToApplicationDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Başvurular getirildi", dtoList));
    }

    @GetMapping("/{tournamentId}/participants")
    @Operation(summary = "Turnuvaya katılan onaylı takımları (Asil Kadro) listele")
    public ResponseEntity<ApiResponse<List<TournamentApplicationResponseDTO>>> getTournamentParticipants(@PathVariable Long tournamentId) {
        List<TournamentApplicationResponseDTO> dtoList = turnuvaService.getTournamentParticipants(tournamentId).stream()
                .map(this::mapToApplicationDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva katılımcıları getirildi", dtoList));
    }

    @GetMapping("/my/applications")
    @Operation(summary = "Kendi başvurularımı listele")
    public ResponseEntity<ApiResponse<List<TournamentApplicationResponseDTO>>> getMyApplications() {
        Long userId = getCurrentUserId();
        List<TournamentApplicationResponseDTO> dtoList = turnuvaService.getUserApplications(userId).stream()
                .map(this::mapToApplicationDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Başvurularınız getirildi", dtoList));
    }

    @PutMapping("/{tournamentId}/deadline")
    @Operation(summary = "Kayıt Süresini Güncelle")
    public ResponseEntity<ApiResponse<TurnuvaResponseDTO>> updateTournamentDeadline(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date newDeadline) {

        Turnuva updatedTurnuva = turnuvaService.updateTournamentDeadline(tournamentId, newDeadline);
        return ResponseEntity.ok(new ApiResponse<>(true, "Süre güncellendi", mapToTurnuvaDTO(updatedTurnuva)));
    }

    @PostMapping("/{tournamentId}/invite/{targetUserId}")
    @Operation(summary = "VIP Davet At")
    public ResponseEntity<ApiResponse<TournamentApplicationResponseDTO>> inviteUserToTournament(
            @PathVariable Long tournamentId,
            @PathVariable Long targetUserId) {

        TournamentApplication application = turnuvaService.inviteUserToTournament(tournamentId, targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Davet gönderildi", mapToApplicationDTO(application)));
    }

    @PostMapping("/applications/{applicationId}/respond-invite")
    @Operation(summary = "Daveti Yanıtla (Onayla / Reddet / Yedek Ol)")
    public ResponseEntity<ApiResponse<TournamentApplicationResponseDTO>> respondToTournamentInvite(
            @PathVariable Long applicationId,
            @Valid @RequestBody RespondToTournamentInviteRequest request) {

        TournamentApplication application = turnuvaService.respondToTournamentInvite(applicationId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Davet yanıtlandı", mapToApplicationDTO(application)));
    }

    @PostMapping("/{tournamentId}/check-in")
    @Operation(summary = "Yoklama Yap")
    public ResponseEntity<ApiResponse<TournamentApplicationResponseDTO>> performCheckIn(
            @PathVariable Long tournamentId) {

        TournamentApplication application = turnuvaService.performCheckIn(tournamentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Check-in başarılı", mapToApplicationDTO(application)));
    }

    @PostMapping("/{tournamentId}/finish")
    @Operation(summary = "Turnuvayı Bitir ve Ödülleri Dağıt", description = "Sadece organizatör yapabilir. Ödül havuzundaki MEYDAN_COIN'ler, kazanan takımların oyuncularına eşit paylaştırılır.")
    public ResponseEntity<ApiResponse<Void>> finishTournament(
            @PathVariable Long tournamentId,
            @Valid @RequestBody RewardReportRequest request) {

        turnuvaService.finishTournamentAndDistributeRewards(tournamentId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva başarıyla bitirildi ve ödüller cüzdanlara dağıtıldı.", null));
    }
}