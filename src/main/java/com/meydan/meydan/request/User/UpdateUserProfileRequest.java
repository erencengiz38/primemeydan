package com.meydan.meydan.request.User;

import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String display_name;
    private String bio;
    private String profile_picture_url;
    private String banner_url;
}
