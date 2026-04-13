package com.meydan.meydan.controller;

import com.meydan.meydan.config.CurrentUserId;
import com.meydan.meydan.dto.ApiResponse;
import com.meydan.meydan.dto.Turnuva.UpdateApplicationStatusRequestBody;
import com.meydan.meydan.dto.response.OrganizationMemberResponseDTO;
import com.meydan.meydan.dto.response.OrganizationQuotaResponseDTO;
import com.meydan.meydan.dto.response.OrganizationResponseDTO;
import com.meydan.meydan.models.entities.Organization;
import com.meydan.meydan.models.entities.OrganizationApplication;
import com.meydan.meydan.models.entities.OrganizationMembership;
import com.meydan.meydan.request.Organization.CreateOrganizationRequestBody;
import com.meydan.meydan.service.OrganizationApplicationService;
import com.meydan.meydan.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organization", description = "Organizasyon ve Başvuru API endpoint'leri")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationApplicationService applicationService;
    private final ModelMapper modelMapper;

    private OrganizationMemberResponseDTO mapToOrganizationMemberDTO(OrganizationMembership member) {
        OrganizationMemberResponseDTO dto = new OrganizationMemberResponseDTO();
        dto.setUserId(member.getUser().getId());
        dto.setUserName(member.getUser().getDisplay_name());
        dto.setUserTag(member.getUser().getTag());
        dto.setRole(member.getRole());
        dto.setOrganizationId(member.getOrganization().getId());
        dto.setOrganizationName(member.getOrganization().getName());
        return dto;
    }

    @GetMapping("/list")
    @Operation(summary = "Tüm organizasyonları listele")
    public ResponseEntity<ApiResponse<List<OrganizationResponseDTO>>> getAllOrganizations() {
        List<OrganizationResponseDTO> dtoList = organizationService.getAllOrganizations().stream()
                .map(org -> modelMapper.map(org, OrganizationResponseDTO.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Organizasyonlar başarıyla getirildi", dtoList));
    }

    @GetMapping("/{organizationId}/members")
    @Operation(summary = "Organizasyon üyelerini listele")
    public ResponseEntity<ApiResponse<List<OrganizationMemberResponseDTO>>> getOrganizationMembers(@PathVariable Long organizationId) {
        List<OrganizationMemberResponseDTO> dtoList = organizationService.getOrganizationMembers(organizationId).stream()
                .map(this::mapToOrganizationMemberDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Organizasyon üyeleri başarıyla getirildi", dtoList));
    }

    @PostMapping("/create")
    @Operation(summary = "Yeni organizasyon oluştur")
    public ResponseEntity<Long> createOrganization(
            @RequestBody CreateOrganizationRequestBody request,
            @Parameter(hidden = true) @CurrentUserId Long creatorId) {

        Long newOrgId = organizationService.createOrganization(request, creatorId);
        return ResponseEntity.ok(newOrgId);
    }

    @PostMapping("/apply")
    @Operation(summary = "Organizasyona katılmak için başvur")
    public ResponseEntity<Void> applyToOrganization(
            @RequestParam Long organizationId,
            @RequestBody String message,
            @CurrentUserId Long userId) {

        applicationService.applyToOrganization(organizationId, userId, message);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{organizationId}/applications/pending")
    @Operation(summary = "Organizasyonun bekleyen başvurularını listele (Yetkili)")
    public ResponseEntity<List<OrganizationApplication>> getPendingApplications(
            @PathVariable Long organizationId,
            @CurrentUserId Long requesterId) {

        List<OrganizationApplication> applications = applicationService.getPendingApplications(organizationId, requesterId);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{organizationId}/applications")
    @Operation(summary = "Organizasyonun tüm geçmiş/bekleyen başvurularını listele (Yetkili)")
    public ResponseEntity<List<OrganizationApplication>> getAllApplications(
            @PathVariable Long organizationId,
            @CurrentUserId Long requesterId) {

        List<OrganizationApplication> applications = applicationService.getAllApplications(organizationId, requesterId);
        return ResponseEntity.ok(applications);
    }

    @PutMapping("/applications/{applicationId}/status")
    @Operation(summary = "Organizasyon başvurusunu onayla veya reddet (Yetkili)")
    public ResponseEntity<Void> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestBody UpdateApplicationStatusRequestBody request,
            @CurrentUserId Long approverId) {

        applicationService.updateApplicationStatus(applicationId, approverId, request.getStatus());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/quota")
    @Operation(summary = "Organizasyonun haftalık kotasını getir")
    public ResponseEntity<ApiResponse<OrganizationQuotaResponseDTO>> getOrganizationQuota(
            @PathVariable Long id,
            @CurrentUserId Long requesterId) {

        OrganizationQuotaResponseDTO dto = organizationService.getOrganizationQuota(id, requesterId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Kota getirildi", dto));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Organizasyondan üye/yönetici kov (Sadece OWNER)")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @CurrentUserId Long requesterId) {

        organizationService.removeMember(id, userId, requesterId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Üye başarıyla çıkarıldı", null));
    }
}
