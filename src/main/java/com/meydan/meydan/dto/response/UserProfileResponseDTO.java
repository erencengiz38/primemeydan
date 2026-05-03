package com.meydan.meydan.dto.response;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserProfileResponseDTO {
    private Long id;
    private UUID oid;
    private String display_name;
    private String tag;
    private String bio;
    private String profile_picture_url;
    private String banner_url;
    private Boolean isPrivate;

    // Klan Bilgileri
    private List<ClanProfileDTO> clans;

    // Ratingler/Kritikler
    private List<UserRatingDTO> ratings;

    // Settings (Sadece kendi profilinde göster)
    private UserProfileSettingsDTO settings;

    @Data
    public static class ClanProfileDTO {
        private Long clanId;
        private String clanName;
        private String clanRole; // OWNER, MANAGER, MEMBER
        private Long categoryId;
        private String categoryName;
    }

    @Data
    public static class UserRatingDTO {
        private Long ratingId;
        private String raterName;
        private Integer score; // 1-5
        private String comment;
        private String createdAt;
    }

    @Data
    public static class UserProfileSettingsDTO {
        private Boolean showProfile;
        private Boolean showClans;
        private Boolean showRatings;
        private Boolean showBio;
        private Boolean allowDirectMessages;
        private Boolean isPrivate;
    }
}
