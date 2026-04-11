package com.vinhtran.tram.controller;

import com.vinhtran.tram.dto.StarRequest;
import com.vinhtran.tram.dto.StarResponse;
import com.vinhtran.tram.service.StarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stars")
@RequiredArgsConstructor
public class StarController {

    private final StarService starService;

    @GetMapping
    public ResponseEntity<List<StarResponse>> getAll() {
        return ResponseEntity.ok(starService.getAllStars());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody StarRequest req, Authentication auth) {
        try {
            String nickname = auth != null ? auth.getName() : null;
            StarResponse res = starService.createStar(req, nickname);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/react")
    public ResponseEntity<?> react(@PathVariable Long id,
                                   @RequestBody Map<String, String> body,
                                   Authentication auth) {
        try {
            String type = body.get("type");
            if (type == null || !List.of("listen", "hug", "strong").contains(type)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Loại reaction không hợp lệ"));
            }
            String nickname = auth != null ? auth.getName() : null;
            starService.addReaction(id, type, nickname);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}