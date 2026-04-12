package com.vinhtran.tram.controller;

import com.vinhtran.tram.dto.AuthRequest;
import com.vinhtran.tram.dto.AuthResponse;
import com.vinhtran.tram.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    // ✅ Cần JWT token
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        try {
            AuthResponse res = authService.getMe(auth.getName());
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ✅ Cần JWT token
    @PostMapping("/unlock")
    public ResponseEntity<?> unlock(@RequestBody Map<String, String> body, Authentication auth) {
        try {
            String itemId = body.get("itemId");
            String nickname = auth.getName();
            AuthResponse res = authService.unlockItem(nickname, itemId);
            return ResponseEntity.ok(res);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}