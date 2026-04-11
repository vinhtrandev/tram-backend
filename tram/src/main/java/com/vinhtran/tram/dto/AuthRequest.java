package com.vinhtran.tram.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String nickname;
    private String password;
}