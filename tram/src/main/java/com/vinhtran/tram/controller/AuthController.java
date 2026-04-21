package com.vinhtran.tram.controller;

import com.vinhtran.tram.dto.AuthRequest;
import com.vinhtran.tram.dto.AuthResponse;
import com.vinhtran.tram.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // FIX: Bỏ hết try/catch — GlobalExceptionHandler đã xử lý tập trung
    // FIX: Thêm kiểm tra auth null/isAuthenticated() ở tất cả endpoint cần login

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        return ResponseEntity.ok(authService.getMe(auth.getName()));
    }

    @PostMapping("/unlock")
    public ResponseEntity<?> unlock(@RequestBody Map<String, String> body, Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        String itemId = body.get("itemId");
        if (itemId == null || itemId.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu itemId"));
        return ResponseEntity.ok(authService.unlockItem(auth.getName(), itemId));
    }

    @PostMapping("/points")
    public ResponseEntity<?> addPoints(@RequestBody Map<String, Object> body, Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));

        // FIX: Map<String,Long> gây ClassCastException vì JSON Integer != Long
        // Dùng Map<String,Object> rồi parse an toàn
        long amount;
        try {
            Object raw = body.get("amount");
            if (raw == null)
                return ResponseEntity.badRequest().body(Map.of("message", "Số điểm không hợp lệ"));
            amount = ((Number) raw).longValue();
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số điểm không hợp lệ"));
        }

        if (amount <= 0)
            return ResponseEntity.badRequest().body(Map.of("message", "Số điểm không hợp lệ"));
        if (amount > 500)
            return ResponseEntity.badRequest().body(Map.of("message", "Số điểm vượt giới hạn"));

        return ResponseEntity.ok(authService.addPoints(auth.getName(), amount));
    }

    @PostMapping("/streak")
    public ResponseEntity<?> syncStreak(@RequestBody Map<String, List<String>> body, Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        List<String> dates = body.get("dates");
        if (dates == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu dates"));
        return ResponseEntity.ok(authService.syncStreak(auth.getName(), dates));
    }
}