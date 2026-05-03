package com.meydan.meydan.controller;

import com.meydan.meydan.dto.ApiResponse;
import com.meydan.meydan.dto.response.UserProfileResponseDTO;
import com.meydan.meydan.dto.response.UserResponseDTO;
import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.models.entities.UserProfileSettings;
import com.meydan.meydan.request.User.UpdateProfileSettingsRequest;
import com.meydan.meydan.request.User.UpdateUserProfileRequest;
import com.meydan.meydan.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "Kullanıcı API endpoint'leri")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/available-for-clan")
    @Operation(summary = "Klansız kullanıcıları listele", description = "Belirli bir oyun kategorisinde herhangi bir klanı olmayan kullanıcıları sayfalı olarak getirir.")
    public ResponseEntity<ApiResponse<Page<UserResponseDTO>>> getAvailableUsersForClan(
            @RequestParam Long categoryId,
            Pageable pageable) {
        Page<UserResponseDTO> users = userService.findAvailableUsersForClan(categoryId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Klana eklenebilir kullanıcılar başarıyla getirildi.", users));
    }

    @GetMapping("/{userId}/profile")
    @Operation(summary = "Kullanıcı profilini görüntüle", description = "Başka bir kullanıcının veya kendi profilini görüntüle. Cüzdan bilgileri gösterilmez.")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getUserProfile(@PathVariable Long userId) {
        UserProfileResponseDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profil başarıyla getirildi.", profile));
    }

    @GetMapping("/me/profile")
    @Operation(summary = "Kendi profilimi görüntüle", description = "Giriş yapan kullanıcı kendi profilini ve ayarlarını görebilir.")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getMyProfile() {
        // SecurityContext'ten user ID'yi al - getCurrentUserId'nin implemetasyonu
        org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        UserProfileResponseDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Kendi profiliniz getirildi.", profile));
    }

    @PutMapping("/profile/settings")
    @Operation(summary = "Profil ayarlarını güncelle", description = "Bio, gizlilik ayarları ve görünürlük tercihlerini güncelle.")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO.UserProfileSettingsDTO>> updateProfileSettings(
            @RequestBody UpdateProfileSettingsRequest request) {
        UserProfileSettings settings = userService.updateProfileSettings(request);

        UserProfileResponseDTO.UserProfileSettingsDTO settingsDTO = new UserProfileResponseDTO.UserProfileSettingsDTO();
        settingsDTO.setShowProfile(settings.getShowProfile());
        settingsDTO.setShowClans(settings.getShowClans());
        settingsDTO.setShowRatings(settings.getShowRatings());
        settingsDTO.setShowBio(settings.getShowBio());
        settingsDTO.setAllowDirectMessages(settings.getAllowDirectMessages());
        settingsDTO.setIsPrivate(settings.getIsPrivate());

        return ResponseEntity.ok(new ApiResponse<>(true, "Profil ayarları başarıyla güncellendi.", settingsDTO));
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Profil bilgilerini güncelle", description = "Display name, bio, profil fotoğrafı ve banner'ı güncelle.")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> updateMyProfile(
            @RequestBody UpdateUserProfileRequest request) {
        User updatedUser = userService.updateUserProfile(request);

        // Güncellenmiş profili getir
        Long userId = Long.parseLong(
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
        );
        UserProfileResponseDTO profile = userService.getUserProfile(userId);

        return ResponseEntity.ok(new ApiResponse<>(true, "Profil başarıyla güncellendi.", profile));
    }
}