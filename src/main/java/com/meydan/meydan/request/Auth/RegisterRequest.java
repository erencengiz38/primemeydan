package com.meydan.meydan.request.Auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String mail;
    private String password;
    private String display_name;
    private String tag;
}
