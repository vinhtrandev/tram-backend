package com.vinhtran.tram.dto;

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
}