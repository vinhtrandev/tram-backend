package com.vinhtran.tram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String nickname;
    private String token;
    private long points;
}