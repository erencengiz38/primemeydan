package com.meydan.meydan.controller;

import com.meydan.meydan.dto.Turnuva.UpdateApplicationStatusRequestBody;
import com.meydan.meydan.models.entities.OrganizationApplication;
import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.repository.UserRepository;
import com.meydan.meydan.service.OrganizationApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class OrganizationApplicationController {

    private final OrganizationApplicationService applicationService;
    private final UserRepository userRepository;

    // JWT'den (Security Context) gelen kullanıcıyı bulup güvenli bir şekilde ID'sini aldığımız yardımcı metod
    private Long getAuthenticatedUserId(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Yetkisiz işlem: Lütfen giriş yapın.");
        }
        User user = userRepository.findByMail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Token'a ait kullanıcı veritabanında bulunamadı!"));
        return user.getId();
    }

    @PostMapping("/apply")
    public ResponseEntity<Void> applyToOrganization(
            @RequestParam Long organizationId,
            @RequestBody String message,
            Principal principal) {

        Long userId = getAuthenticatedUserId(principal);
        applicationService.applyToOrganization(organizationId, userId, message);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending/{organizationId}")
    public ResponseEntity<List<OrganizationApplication>> getPendingApplications(
            @PathVariable Long organizationId,
            Principal principal) {

        Long requesterId = getAuthenticatedUserId(principal);
        List<OrganizationApplication> applications = applicationService.getPendingApplications(organizationId, requesterId);
        return ResponseEntity.ok(applications);
    }

    @PutMapping("/status/{applicationId}")
    public ResponseEntity<Void> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestBody UpdateApplicationStatusRequestBody request,
            Principal principal) {

        Long approverId = getAuthenticatedUserId(principal);
        applicationService.updateApplicationStatus(applicationId, approverId, request.getStatus());
        return ResponseEntity.ok().build();
    }
}