package com.meydan.meydan.controller;


import com.meydan.meydan.request.Auth.Turnuva.AddTurnuvaRequestBody;
import com.meydan.meydan.request.Auth.Turnuva.UpdateTurnuvaRequestBody;
import com.meydan.meydan.request.Auth.Turnuva.ApplyToTournamentRequestBody;
import com.meydan.meydan.request.Auth.Turnuva.UpdateApplicationStatusRequestBody;
import com.meydan.meydan.dto.ApiResponse;
import com.meydan.meydan.models.entities.Turnuva;
import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.service.TurnuvaService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/turnuva")
@Tag(name = "Turnuva", description = "Turnuva API endpoint'leri")
@RequiredArgsConstructor
public class TurnuvaController {
    private final TurnuvaService turnuvaService;

    @PostMapping("/create")
    @Operation(summary = "Yeni turnuva oluştur", description = "JWT token'dan kullanıcının organizasyon ID'si alınır - IDOR koruması")
    public ResponseEntity<ApiResponse<Turnuva>> createTurnuva(@Valid @RequestBody AddTurnuvaRequestBody addTurnuvaRequestBody) {
        // JWT token'dan kullanıcının ID'sini al (organizationId olarak kullanılır)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Kullanıcı kimlik doğrulaması gerekli", null));
        }
        
        User currentUser = (User) authentication.getPrincipal();
        Long organizationId = currentUser.getId(); // User ID'sini organizationId olarak kullan

        Turnuva turnuva = turnuvaService.createTurnuva(addTurnuvaRequestBody, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Turnuva başarıyla oluşturuldu", turnuva));
    }

    @GetMapping("/list")
    @Operation(summary = "Tüm turnuvaları listele", description = "Veritabanındaki tüm aktif turnuvaları getirir")
    public ResponseEntity<ApiResponse<List<Turnuva>>> getAllTurnuvas() {
        List<Turnuva> turnuvas = turnuvaService.getAllTurnuvas();
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuvalar başarıyla getirildi", turnuvas));
    }

    @GetMapping("/list/paginated")
    @Operation(summary = "Turnuvaları sayfalı olarak listele", description = "Veritabanındaki tüm aktif turnuvaları sayfalı şekilde getirir")
    public ResponseEntity<ApiResponse<Page<Turnuva>>> getAllTurnuvasWithPagination(Pageable pageable) {
        Page<Turnuva> turnuvas = turnuvaService.getAllTurnuvasWithPagination(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuvalar başarıyla getirildi", turnuvas));
    }

    @GetMapping("/{organizationId}")
    @Operation(summary = "Belirli organizasyonun turnuvalarını listele",
               description = "Verilen organizationId'ye sahip tüm turnuvaları getirir")
    public ResponseEntity<ApiResponse<List<Turnuva>>> getTurnuvasByOrganization(@PathVariable Long organizationId) {
        List<Turnuva> turnuvas = turnuvaService.getTurnuvasByOrganizationId(organizationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Organizasyonun turnuvaları başarıyla getirildi", turnuvas));
    }

    @GetMapping("/{organizationId}/paginated")
    @Operation(summary = "Belirli organizasyonun turnuvalarını sayfalı olarak listele",
               description = "Verilen organizationId'ye sahip tüm turnuvaları sayfalı şekilde getirir")
    public ResponseEntity<ApiResponse<Page<Turnuva>>> getTurnuvasByOrganizationWithPagination(
            @PathVariable Long organizationId, Pageable pageable) {
        Page<Turnuva> turnuvas = turnuvaService.getTurnuvasByOrganizationIdWithPagination(organizationId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Organizasyonun turnuvaları başarıyla getirildi", turnuvas));
    }

    @GetMapping("/my")
    @Operation(summary = "Kendi turnuvalarımı listele", 
               description = "JWT token'dan kullanıcının kendi organizasyonuna ait turnuvaları getirir")
    public ResponseEntity<ApiResponse<List<Turnuva>>> getMyTurnuvas() {
        // JWT token'dan kullanıcının ID'sini al
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Kullanıcı kimlik doğrulaması gerekli", null));
        }
        
        User currentUser = (User) authentication.getPrincipal();
        Long organizationId = currentUser.getId();
        
        List<Turnuva> turnuvas = turnuvaService.getTurnuvasByOrganizationId(organizationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Kendi turnuvalarınız başarıyla getirildi", turnuvas));
    }

    @GetMapping("/my/paginated")
    @Operation(summary = "Kendi turnuvalarımı sayfalı olarak listele",
               description = "JWT token'dan kullanıcının kendi organizasyonuna ait turnuvaları sayfalı şekilde getirir")
    public ResponseEntity<ApiResponse<Page<Turnuva>>> getMyTurnuvasWithPagination(Pageable pageable) {
        // JWT token'dan kullanıcının ID'sini al
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Kullanıcı kimlik doğrulaması gerekli", null));
        }

        User currentUser = (User) authentication.getPrincipal();
        Long organizationId = currentUser.getId();

        Page<Turnuva> turnuvas = turnuvaService.getTurnuvasByOrganizationIdWithPagination(organizationId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Kendi turnuvalarınız başarıyla getirildi", turnuvas));
    }

    @PutMapping("/update")
    @Operation(summary = "Turnuvayı güncelle", description = "JWT token'dan kullanıcının organizasyon ID'si alınır - IDOR koruması")
    public ResponseEntity<ApiResponse<Turnuva>> updateTurnuva(@Valid @RequestBody UpdateTurnuvaRequestBody updateTurnuvaRequestBody) {
        // JWT token'dan kullanıcının ID'sini al (organizationId olarak kullanılır)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Kullanıcı kimlik doğrulaması gerekli", null));
        }

        User currentUser = (User) authentication.getPrincipal();
        Long organizationId = currentUser.getId();

        Turnuva updatedTurnuva = turnuvaService.updateTurnuva(updateTurnuvaRequestBody, organizationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva başarıyla güncellendi", updatedTurnuva));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Turnuvayı sil", description = "JWT token'dan kullanıcının organizasyon ID'si alınır - IDOR koruması")
    public ResponseEntity<ApiResponse<Turnuva>> deleteTurnuva(@PathVariable Long id) {
        // JWT token'dan kullanıcının ID'sini al
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Kullanıcı kimlik doğrulaması gerekli", null));
        }

        User currentUser = (User) authentication.getPrincipal();
        Long organizationId = currentUser.getId();

        Turnuva deletedTurnuva = turnuvaService.deleteTurnuva(id, organizationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva başarıyla silindi", deletedTurnuva));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Turnuvayı geri yükle", description = "JWT token'dan kullanıcının organizasyon ID'si alınır - IDOR koruması")
    public ResponseEntity<ApiResponse<Turnuva>> restoreTurnuva(@PathVariable Long id) {
        // JWT token'dan kullanıcının ID'sini al
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Kullanıcı kimlik doğrulaması gerekli", null));
        }

        User currentUser = (User) authentication.getPrincipal();
        Long organizationId = currentUser.getId();

        Turnuva restoredTurnuva = turnuvaService.restoreTurnuva(id, organizationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva başarıyla geri yüklendi", restoredTurnuva));
    }

    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "Turnuvayı kalıcı olarak sil", description = "JWT token'dan kullanıcının organizasyon ID'si alınır - IDOR koruması")
    public ResponseEntity<ApiResponse<Turnuva>> permanentlyDeleteTurnuva(@PathVariable Long id) {
        // JWT token'dan kullanıcının ID'sini al
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Kullanıcı kimlik doğrulaması gerekli", null));
        }

        User currentUser = (User) authentication.getPrincipal();
        Long organizationId = currentUser.getId();

        Turnuva deletedTurnuva = turnuvaService.permanentlyDeleteTurnuva(id, organizationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva kalıcı olarak silindi", deletedTurnuva));
    }

    @PostMapping("/apply")
    @Operation(summary = "Turnuvaya başvur", description = "SOLO veya CLAN turnuvasına başvur (ParticipantType'a göre)")
    public ResponseEntity<ApiResponse<com.meydan.meydan.models.entities.TournamentApplication>> applyToTournament(@Valid @RequestBody ApplyToTournamentRequestBody request) {
        // JWT token'dan applicant user ID'yi al
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Kullanıcı kimlik doğrulaması gerekli", null));
        }

        User currentUser = (User) authentication.getPrincipal();
        Long applicantUserId = currentUser.getId();

        com.meydan.meydan.models.entities.TournamentApplication application = turnuvaService.applyToTournament(request, applicantUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Turnuva başvurusu başarıyla oluşturuldu", application));
    }

    @PutMapping("/application/{applicationId}/status")
    @Operation(summary = "Başvuru durumunu güncelle", description = "Admin tarafından başvuru durumunu APPROVED/REJECTED olarak günceller")
    public ResponseEntity<ApiResponse<com.meydan.meydan.models.entities.TournamentApplication>> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequestBody request) {
        // JWT token'dan admin user ID'yi al
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Kullanıcı kimlik doğrulaması gerekli", null));
        }

        User currentUser = (User) authentication.getPrincipal();
        Long adminUserId = currentUser.getId();

        com.meydan.meydan.models.entities.TournamentApplication application = turnuvaService.updateApplicationStatus(applicationId, request, adminUserId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Başvuru durumu başarıyla güncellendi", application));
    }

    @GetMapping("/{tournamentId}/applications")
    @Operation(summary = "Turnuva başvurularını listele", description = "Belirli bir turnuvanın tüm başvurularını getirir")
    public ResponseEntity<ApiResponse<List<com.meydan.meydan.models.entities.TournamentApplication>>> getTournamentApplications(@PathVariable Long tournamentId) {
        List<com.meydan.meydan.models.entities.TournamentApplication> applications = turnuvaService.getTournamentApplications(tournamentId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Turnuva başvuruları başarıyla getirildi", applications));
    }

    @GetMapping("/my/applications")
    @Operation(summary = "Kendi başvurularımı listele", description = "JWT token'dan kullanıcının tüm turnuva başvurularını getirir")
    public ResponseEntity<ApiResponse<List<com.meydan.meydan.models.entities.TournamentApplication>>> getMyApplications() {
        // JWT token'dan user ID'yi al
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Kullanıcı kimlik doğrulaması gerekli", null));
        }

        User currentUser = (User) authentication.getPrincipal();
        Long userId = currentUser.getId();

        List<com.meydan.meydan.models.entities.TournamentApplication> applications = turnuvaService.getUserApplications(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Başvurularınız başarıyla getirildi", applications));
    }
}
