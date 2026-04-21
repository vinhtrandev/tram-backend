package com.vinhtran.tram.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StarRequest {

    @NotBlank(message = "Nội dung không được trống")
    @Size(max = 200, message = "Tối đa 200 ký tự")
    private String text;

    private String type;
    private double x;
    private double y;

    @JsonAlias("isMoodPost")
    private Boolean moodPost = false;

    private String nickname;

    // THÊM
    private Double size       = 4.0;
    private Double opacity    = 0.85;
    private Boolean haloEffect = false;
    private Boolean tailEffect = false;
}