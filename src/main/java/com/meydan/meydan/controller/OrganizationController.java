package com.meydan.meydan.controller;

import com.meydan.meydan.config.CurrentUserId;
import com.meydan.meydan.dto.Turnuva.UpdateApplicationStatusRequestBody;
import com.meydan.meydan.models.entities.OrganizationApplication;
import com.meydan.meydan.request.Organization.CreateOrganizationRequestBody;
import com.meydan.meydan.service.OrganizationApplicationService;
import com.meydan.meydan.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organization", description = "Organizasyon ve Başvuru API endpoint'leri")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationApplicationService applicationService;

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

    @PutMapping("/applications/{applicationId}/status")
    @Operation(summary = "Organizasyon başvurusunu onayla veya reddet (Yetkili)")
    public ResponseEntity<Void> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestBody UpdateApplicationStatusRequestBody request,
            @CurrentUserId Long approverId) {

        applicationService.updateApplicationStatus(applicationId, approverId, request.getStatus());
        return ResponseEntity.ok().build();
    }
}
