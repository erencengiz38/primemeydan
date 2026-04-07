package com.meydan.meydan.request.Auth;


import lombok.Data;

@Data
public class LoginRequest {
    private String mail;
    private String password;
}
