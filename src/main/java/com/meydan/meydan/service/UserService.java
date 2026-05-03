package com.meydan.meydan.service;

import com.meydan.meydan.dto.response.UserProfileResponseDTO;
import com.meydan.meydan.dto.response.UserResponseDTO;
import com.meydan.meydan.exception.BaseException;
import com.meydan.meydan.exception.ErrorCode;
import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.repository.*;
import com.meydan.meydan.request.User.UpdateProfileSettingsRequest;
import com.meydan.meydan.request.User.UpdateUserProfileRequest;
import com.meydan.meydan.util.XssSanitizer;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final UserProfileSettingsRepository profileSettingsRepository;
    private final ClanMemberRepository clanMemberRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final XssSanitizer xssSanitizer;

    // Helper: Get Current User ID
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BaseException(
                    ErrorCode.AUTH_002,
                    "Kullanıcı oturumu bulunamadı. Lütfen giriş yapın.",
                    HttpStatus.UNAUTHORIZED,
                    ""
            );
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BaseException(
                    ErrorCode.AUTH_003,
                    "Kullanıcı kimliği doğrulanamadı.",
                    HttpStatus.UNAUTHORIZED,
                    ""
            );
        }
    }

    public Page<UserResponseDTO> findAvailableUsersForClan(Long categoryId, Pageable pageable) {
        return userRepository.findUsersNotInClanByCategory(categoryId, pageable)
                .map(user -> modelMapper.map(user, UserResponseDTO.class));
    }

    @Transactional
    public UserProfileSettings getOrCreateProfileSettings(Long userId) {
        return profileSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfileSettings settings = new UserProfileSettings(userId);
                    return profileSettingsRepository.save(settings);
                });
    }

    @Transactional
    public UserProfileResponseDTO getUserProfile(Long targetUserId) {
        Long currentUserId = null;
        boolean isOwnProfile = false;

        try {
            currentUserId = getCurrentUserId();
            isOwnProfile = currentUserId.equals(targetUserId);
        } catch (BaseException e) {
            // Kullanıcı giriş yapmamışsa
            isOwnProfile = false;
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SYS_001,
                        "Kullanıcı bulunamadı.",
                        HttpStatus.NOT_FOUND,
                        "User ID: " + targetUserId
                ));

        UserProfileSettings settings = getOrCreateProfileSettings(targetUserId);

        // Profil gizliyse kontrol et
        if (!isOwnProfile && settings.getIsPrivate()) {
            throw new BaseException(
                    ErrorCode.AUTH_001,
                    "Bu profil gizlidir. Erişim yetkiniz yok.",
                    HttpStatus.FORBIDDEN,
                    "User ID: " + targetUserId
            );
        }

        UserProfileResponseDTO profileDTO = new UserProfileResponseDTO();
        profileDTO.setId(user.getId());
        profileDTO.setOid(user.getOid());
        profileDTO.setDisplay_name(user.getDisplay_name());
        profileDTO.setTag(user.getTag());
        profileDTO.setIsPrivate(settings.getIsPrivate());

        // Dinamik olarak MediaAsset tablosundan Profil Fotoğrafı (USER_AVATAR) çekme ve DTO'ya setleme
        List<MediaAsset> avatars = mediaAssetRepository.findByAssetTypeAndRelatedId("USER_AVATAR", String.valueOf(targetUserId));
        if (avatars != null && !avatars.isEmpty()) {
            String latestAvatarUrl = avatars.stream()
                    .max(Comparator.comparing(MediaAsset::getCreatedAt))
                    .map(MediaAsset::getImageUrl)
                    .orElse(user.getProfile_picture_url());
            profileDTO.setProfile_picture_url(latestAvatarUrl);
        } else {
            profileDTO.setProfile_picture_url(user.getProfile_picture_url()); // Media asset yoksa User entity'sindeki fallback değeri kullan
        }

        // Dinamik olarak MediaAsset tablosundan Banner (USER_BANNER) çekme ve DTO'ya setleme
        List<MediaAsset> banners = mediaAssetRepository.findByAssetTypeAndRelatedId("USER_BANNER", String.valueOf(targetUserId));
        if (banners != null && !banners.isEmpty()) {
            String latestBannerUrl = banners.stream()
                    .max(Comparator.comparing(MediaAsset::getCreatedAt))
                    .map(MediaAsset::getImageUrl)
                    .orElse(user.getBanner_url());
            profileDTO.setBanner_url(latestBannerUrl);
        } else {
            profileDTO.setBanner_url(user.getBanner_url()); // Media asset yoksa User entity'sindeki fallback değeri kullan
        }

        // Bio göster
        if (settings.getShowBio()) {
            profileDTO.setBio(settings.getBio());
        }

        // Klan bilgileri göster
        if (settings.getShowClans()) {
            List<ClanMember> userClans = clanMemberRepository.findUserActiveClans(targetUserId);
            List<UserProfileResponseDTO.ClanProfileDTO> clanDTOs = userClans.stream()
                    .map(cm -> {
                        UserProfileResponseDTO.ClanProfileDTO clanDTO = new UserProfileResponseDTO.ClanProfileDTO();
                        clanDTO.setClanId(cm.getClan().getId());
                        clanDTO.setClanName(cm.getClan().getName());
                        clanDTO.setClanRole(cm.getRole().toString());
                        clanDTO.setCategoryId(cm.getClan().getCategory().getId());
                        clanDTO.setCategoryName(cm.getClan().getCategory().getName());
                        return clanDTO;
                    })
                    .collect(Collectors.toList());
            profileDTO.setClans(clanDTOs);
        } else {
            profileDTO.setClans(List.of());
        }

        // Ratings göster (Henüz Rating entity'si yoksa boş liste dön)
        if (settings.getShowRatings()) {
            profileDTO.setRatings(List.of());
        }

        // Settings sadece kendi profilinde göster
        if (isOwnProfile) {
            UserProfileResponseDTO.UserProfileSettingsDTO settingsDTO = new UserProfileResponseDTO.UserProfileSettingsDTO();
            settingsDTO.setShowProfile(settings.getShowProfile());
            settingsDTO.setShowClans(settings.getShowClans());
            settingsDTO.setShowRatings(settings.getShowRatings());
            settingsDTO.setShowBio(settings.getShowBio());
            settingsDTO.setAllowDirectMessages(settings.getAllowDirectMessages());
            settingsDTO.setIsPrivate(settings.getIsPrivate());
            profileDTO.setSettings(settingsDTO);
        }

        return profileDTO;
    }

    @Transactional
    public UserProfileSettings updateProfileSettings(UpdateProfileSettingsRequest request) {
        Long userId = getCurrentUserId();
        UserProfileSettings settings = getOrCreateProfileSettings(userId);

        if (request.getBio() != null) {
            settings.setBio(xssSanitizer.sanitizeAndLimit(request.getBio(), 500));
        }
        if (request.getShowProfile() != null) {
            settings.setShowProfile(request.getShowProfile());
        }
        if (request.getShowClans() != null) {
            settings.setShowClans(request.getShowClans());
        }
        if (request.getShowRatings() != null) {
            settings.setShowRatings(request.getShowRatings());
        }
        if (request.getShowBio() != null) {
            settings.setShowBio(request.getShowBio());
        }
        if (request.getAllowDirectMessages() != null) {
            settings.setAllowDirectMessages(request.getAllowDirectMessages());
        }
        if (request.getIsPrivate() != null) {
            settings.setIsPrivate(request.getIsPrivate());
        }

        return profileSettingsRepository.save(settings);
    }

    @Transactional
    public User updateUserProfile(UpdateUserProfileRequest request) {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.SYS_001,
                        "Kullanıcı bulunamadı.",
                        HttpStatus.NOT_FOUND,
                        "User ID: " + userId
                ));

        if (request.getDisplay_name() != null && !request.getDisplay_name().trim().isEmpty()) {
            user.setDisplay_name(xssSanitizer.sanitizeAndLimit(request.getDisplay_name(), 100));
        }

        if (request.getBio() != null) {
            UserProfileSettings settings = getOrCreateProfileSettings(userId);
            settings.setBio(xssSanitizer.sanitizeAndLimit(request.getBio(), 500));
            profileSettingsRepository.save(settings);
        }

        if (request.getProfile_picture_url() != null) {
            user.setProfile_picture_url(request.getProfile_picture_url());
        }

        if (request.getBanner_url() != null) {
            user.setBanner_url(request.getBanner_url());
        }

        return userRepository.save(user);
    }
}