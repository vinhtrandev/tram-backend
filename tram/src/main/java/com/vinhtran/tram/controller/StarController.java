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

    /*
     * FIX: Thay @AuthenticationPrincipal Long userId → Authentication
     *
     * @AuthenticationPrincipal Long sẽ không hoạt động vì Spring Security
     * lưu principal là UserDetails object (không phải Long).
     * Cần resolve userId từ UserDetails.getUsername() qua UserRepository,
     * hoặc đơn giản hơn: truyền Authentication xuống Service để resolve.
     *
     * FIX: Thêm @Valid cho StarRequest
     * FIX: Thêm auth null check ở các endpoint cần login
     */

    /** GET /api/stars — public, nhưng nếu đã login thì kèm myReactions */
    @GetMapping
    public ResponseEntity<List<StarResponse>> getStars(Authentication auth) {
        String nickname = (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
        return ResponseEntity.ok(starService.getActiveStars(nickname));
    }

    /** POST /api/stars — tạo star mới */
    @PostMapping
    public ResponseEntity<?> createStar(
            @Valid @RequestBody StarRequest req,
            Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        return ResponseEntity.ok(starService.createStar(req, auth.getName()));
    }

    /** DELETE /api/stars/{id} — xóa nếu hết TTL */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStar(@PathVariable Long id, Authentication auth) {
        // FIX: Không cần auth để xóa sao hết TTL (cleanup)
        // nhưng nên kiểm tra quyền trong service nếu muốn restrict
        starService.deleteIfExpired(id);
        return ResponseEntity.ok().build();
    }

    /** POST /api/stars/{id}/react — thả reaction */
    @PostMapping("/{id}/react")
    public ResponseEntity<?> addReaction(
            @PathVariable Long id,
            @RequestBody ReactionRequest req,
            Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        if (req.type() == null || req.type().isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu type"));
        return ResponseEntity.ok(starService.addReaction(id, req.type(), auth.getName()));
    }

    /** DELETE /api/stars/{id}/react — bỏ reaction */
    @DeleteMapping("/{id}/react")
    public ResponseEntity<?> removeReaction(
            @PathVariable Long id,
            @RequestBody ReactionRequest req,
            Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        if (req.type() == null || req.type().isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu type"));
        return ResponseEntity.ok(starService.removeReaction(id, req.type(), auth.getName()));
    }

    public record ReactionRequest(String type) {}
}