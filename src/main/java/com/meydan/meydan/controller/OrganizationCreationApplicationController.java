package com.meydan.meydan.controller;

import com.meydan.meydan.models.entities.OrganizationCreationApplication;
import com.meydan.meydan.request.OrganizationApplyRequest;
import com.meydan.meydan.request.AdminReviewRequest;
import com.meydan.meydan.service.OrganizationCreationApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organization-applications")
public class OrganizationCreationApplicationController {

    private final OrganizationCreationApplicationService applicationService;

    public OrganizationCreationApplicationController(OrganizationCreationApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getName());
    }

    @PostMapping(value = "/apply", consumes = {"multipart/form-data"})
    public ResponseEntity<String> applyForOrganization(@ModelAttribute OrganizationApplyRequest request) {
        Long userId = getCurrentUserId();
        String result = applicationService.applyForOrganization(userId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-applications")
    public ResponseEntity<List<OrganizationCreationApplication>> getMyApplications() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(applicationService.getMyApplications(userId));
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<List<OrganizationCreationApplication>> getPendingApplications() {
        return ResponseEntity.ok(applicationService.getPendingApplications());
    }

    @PostMapping("/admin/{applicationId}/approve")
    public ResponseEntity<String> approveApplication(
            @PathVariable Long applicationId,
            @RequestBody AdminReviewRequest request) {
        String result = applicationService.approveApplication(applicationId, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/{applicationId}/reject")
    public ResponseEntity<String> rejectApplication(
            @PathVariable Long applicationId,
            @RequestBody AdminReviewRequest request) {
        String result = applicationService.rejectApplication(applicationId, request);
        return ResponseEntity.ok(result);
    }
}