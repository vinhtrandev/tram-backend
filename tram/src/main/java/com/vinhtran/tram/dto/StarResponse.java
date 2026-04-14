package com.vinhtran.tram.dto;

import lombok.Data;

@Data
public class StarResponse {
    private Long id;
    private String text;
    private String type;
    private double x;
    private double y;
    private double size;
    private double opacity;
    private boolean negative;
    private boolean tailEffect;
    private boolean haloEffect;
    private int listenCount;
    private int hugCount;
    private int strongCount;
    private String nickname;
    private String createdAt;
}