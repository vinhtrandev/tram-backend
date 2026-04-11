package com.vinhtran.tram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "Mật danh không được trống")
    @Size(min = 3, max = 50, message = "Mật danh từ 3-50 ký tự")
    private String nickname;

    @NotBlank(message = "Chìa khóa không được trống")
    @Size(min = 6, message = "Chìa khóa ít nhất 6 ký tự")
    private String password;
}