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

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRequest req) {
        try {
            AuthResponse res = authService.register(req);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest req) {
        try {
            AuthResponse res = authService.login(req);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        try {
            AuthResponse res = authService.getMe(auth.getName());
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/unlock")
    public ResponseEntity<?> unlock(@RequestBody Map<String, String> body, Authentication auth) {
        try {
            String itemId = body.get("itemId");
            if (itemId == null || itemId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thiếu itemId"));
            }
            AuthResponse res = authService.unlockItem(auth.getName(), itemId);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // FIX: Endpoint mới - frontend gọi khi hoàn thành mission để cộng điểm vào DB
    @PostMapping("/points")
    public ResponseEntity<?> addPoints(@RequestBody Map<String, Long> body, Authentication auth) {
        try {
            Long amount = body.get("amount");
            if (amount == null || amount <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số điểm không hợp lệ"));
            }
            // Giới hạn tối đa mỗi lần để tránh hack
            if (amount > 500) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số điểm vượt giới hạn"));
            }
            AuthResponse res = authService.addPoints(auth.getName(), amount);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // FIX: Endpoint mới - sync streak từ client lên server
    @PostMapping("/streak")
    public ResponseEntity<?> syncStreak(@RequestBody Map<String, List<String>> body, Authentication auth) {
        try {
            List<String> dates = body.get("dates");
            if (dates == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thiếu dates"));
            }
            AuthResponse res = authService.syncStreak(auth.getName(), dates);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}