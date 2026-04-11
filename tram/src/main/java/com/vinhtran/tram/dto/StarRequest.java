package com.vinhtran.tram.dto;

import lombok.Data;

@Data
public class StarRequest {
    private String text;
    private String type;
    private double x;
    private double y;
    private String nickname;
}