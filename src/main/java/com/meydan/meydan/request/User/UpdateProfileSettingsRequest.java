package com.meydan.meydan.request.User;

import lombok.Data;

@Data
public class UpdateProfileSettingsRequest {
    private String bio;
    private Boolean showProfile;
    private Boolean showClans;
    private Boolean showRatings;
    private Boolean showBio;
    private Boolean allowDirectMessages;
    private Boolean isPrivate;
}

