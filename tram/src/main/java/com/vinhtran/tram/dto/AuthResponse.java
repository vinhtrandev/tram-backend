package com.vinhtran.tram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor  // FIX: thêm để tránh lỗi khi dùng Jackson deserialize hoặc mock trong test
public class AuthResponse {
    private Long id;
    private String nickname;
    private String token;       // null khi gọi /me (không cấp token mới)
    private long points;
    private String unlockedItems;
    private String streakDates;
}