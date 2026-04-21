package com.vinhtran.tram.service;

import com.vinhtran.tram.dto.AuthRequest;
import com.vinhtran.tram.dto.AuthResponse;
import com.vinhtran.tram.entity.User;
import com.vinhtran.tram.repository.UserRepository;
import com.vinhtran.tram.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final Map<String, Integer> ITEM_PRICES = Map.ofEntries(
            Map.entry("trail_star",      300),
            Map.entry("halo_star",       500),
            Map.entry("sound_rain",      100),
            Map.entry("sound_wave",      150),
            Map.entry("sound_wind",      200),
            Map.entry("future_letter",   800),
            Map.entry("meditation",     1000),
            Map.entry("voucher_cafe",   1500),
            Map.entry("gift_box",       3500),
            Map.entry("plant_tree",     5000),
            Map.entry("meal_children",  7000)
    );

    // ══════════════════════════════════════════
    // REGISTER
    // ══════════════════════════════════════════

    @Transactional
    public AuthResponse register(AuthRequest req) {
        if (userRepository.existsByNickname(req.getNickname()))
            throw new RuntimeException("Mật danh đã tồn tại");

        User user = User.builder()
                .nickname(req.getNickname())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();
        user = userRepository.save(user);
        return toAuthResponse(user, jwtUtil.generateToken(user.getNickname()));
    }

    // ══════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════

    @Transactional
    public AuthResponse login(AuthRequest req) {
        User user = userRepository.findByNickname(req.getNickname())
                // FIX: thông báo chung chung để tránh username enumeration attack
                .orElseThrow(() -> new RuntimeException("Sai mật danh hoặc chìa khóa"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
            throw new RuntimeException("Sai mật danh hoặc chìa khóa");

        user.setLastLogin(Instant.now());
        updateStreakOnLogin(user);
        userRepository.save(user);

        return toAuthResponse(user, jwtUtil.generateToken(user.getNickname()));
    }

    // ══════════════════════════════════════════
    // GET ME
    // ══════════════════════════════════════════

    @Transactional(readOnly = true)
    public AuthResponse getMe(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        // FIX: không cấp token mới — client giữ token cũ
        return toAuthResponse(user, null);
    }

    // ══════════════════════════════════════════
    // UNLOCK ITEM
    // ══════════════════════════════════════════

    @Transactional
    public AuthResponse unlockItem(String nickname, String itemId) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Integer price = ITEM_PRICES.get(itemId);
        if (price == null) throw new RuntimeException("Item không tồn tại");
        if (user.getPoints() < price) throw new RuntimeException("Không đủ ✨ Tinh Tú");

        List<String> unlocked = parseCommaList(user.getUnlockedItems());
        if (unlocked.contains(itemId)) throw new RuntimeException("Đã mở khóa rồi");

        unlocked.add(itemId);
        user.setUnlockedItems(String.join(",", unlocked));
        user.addPoints(-price);
        userRepository.save(user);

        return toAuthResponse(user, jwtUtil.generateToken(user.getNickname()));
    }

    // ══════════════════════════════════════════
    // ADD POINTS
    // ══════════════════════════════════════════

    @Transactional
    public AuthResponse addPoints(String nickname, long amount) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        user.addPoints(amount);
        userRepository.save(user);
        // FIX: trả token mới để client cập nhật state
        return toAuthResponse(user, jwtUtil.generateToken(user.getNickname()));
    }

    // ══════════════════════════════════════════
    // SYNC STREAK
    // ══════════════════════════════════════════

    @Transactional
    public AuthResponse syncStreak(String nickname, List<String> clientDates) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        List<String> serverDates = parseCommaList(user.getStreakDates());

        for (String d : clientDates) {
            // FIX: validate format yyyy-MM-dd và tránh inject date rác
            if (d != null && d.matches("\\d{4}-\\d{2}-\\d{2}") && !serverDates.contains(d))
                serverDates.add(d);
        }

        serverDates.sort(String::compareTo);
        // Chỉ giữ 7 ngày gần nhất
        if (serverDates.size() > 7)
            serverDates = serverDates.subList(serverDates.size() - 7, serverDates.size());

        user.setStreakDates(String.join(",", serverDates));
        userRepository.save(user);

        return toAuthResponse(user, jwtUtil.generateToken(user.getNickname()));
    }

    // ══════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════

    /**
     * Cập nhật streak khi user login.
     * FIX: Dùng UTC nhất quán với toàn bộ hệ thống.
     * LocalDate.now() không truyền timezone → phụ thuộc JVM timezone.
     */
    private void updateStreakOnLogin(User user) {
        String today = LocalDate.now(ZoneOffset.UTC).toString(); // "2026-04-20"
        List<String> dates = parseCommaList(user.getStreakDates());
        if (!dates.contains(today)) {
            dates.add(today);
            dates.sort(String::compareTo);
            if (dates.size() > 7)
                dates = dates.subList(dates.size() - 7, dates.size());
            user.setStreakDates(String.join(",", dates));
        }
    }

    /** Parse chuỗi "a,b,c" → List mutable, tránh NPE */
    private List<String> parseCommaList(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(
                user.getId(),
                user.getNickname(),
                token,
                user.getPoints(),
                user.getUnlockedItems() != null ? user.getUnlockedItems() : "",
                user.getStreakDates()   != null ? user.getStreakDates()   : ""
        );
    }
}