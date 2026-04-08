package com.meydan.meydan.controller;

import com.meydan.meydan.config.CurrentUserId;
import com.meydan.meydan.request.Auth.Organization.CreateOrganizationRequestBody;
import com.meydan.meydan.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping("/create")
    public ResponseEntity<Long> createOrganization(
            @RequestBody CreateOrganizationRequestBody request,
            @CurrentUserId Long creatorId) {

        // Servise hem request body'yi hem de resolver'dan gelen ID'yi paslıyoruz
        Long newOrgId = organizationService.createOrganization(request, creatorId);

        return ResponseEntity.ok(newOrgId);
    }
}