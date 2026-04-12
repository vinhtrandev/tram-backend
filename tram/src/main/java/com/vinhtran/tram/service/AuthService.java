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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ID khớp với config.js
    private static final Map<String, Integer> ITEM_PRICES = Map.ofEntries(
            Map.entry("trail_star",     300),
            Map.entry("halo_star",      500),
            Map.entry("sound_rain",     100),
            Map.entry("sound_wave",     150),
            Map.entry("sound_wind",     200),
            Map.entry("future_letter",  800),
            Map.entry("meditation",    1000),
            Map.entry("voucher_cafe",  1500),
            Map.entry("gift_box",      3500),
            Map.entry("plant_tree",    5000),
            Map.entry("meal_children", 7000)
    );

    @Transactional
    public AuthResponse register(AuthRequest req) {
        if (userRepository.existsByNickname(req.getNickname())) {
            throw new RuntimeException("Mật danh đã tồn tại");
        }
        User user = User.builder()
                .nickname(req.getNickname())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getNickname());
        return toAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse login(AuthRequest req) {
        User user = userRepository.findByNickname(req.getNickname())
                .orElseThrow(() -> new RuntimeException("Sai mật danh hoặc chìa khóa"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Sai mật danh hoặc chìa khóa");
        }
        user.setLastLogin(java.time.LocalDateTime.now());
        // FIX: Ghi streak vào DB khi login
        _updateStreakOnServer(user);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getNickname());
        return toAuthResponse(user, token);
    }

    // FIX BUG: getMe dùng readOnly=false vì cần update streak
    @Transactional
    public AuthResponse getMe(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        // FIX: Không generate token mới - dùng lại token cũ từ client
        // Chỉ trả về data user, không cấp token mới để tránh invalidate session
        String token = jwtUtil.generateToken(user.getNickname());
        return toAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse unlockItem(String nickname, String itemId) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Integer price = ITEM_PRICES.get(itemId);
        if (price == null) {
            throw new RuntimeException("Item không tồn tại");
        }

        if (user.getPoints() < price) {
            throw new RuntimeException("Không đủ ✨ Tinh Tú");
        }

        String raw = user.getUnlockedItems() == null ? "" : user.getUnlockedItems();
        List<String> unlocked = new ArrayList<>(
                raw.isEmpty() ? List.of() : List.of(raw.split(","))
        );

        if (unlocked.contains(itemId)) {
            throw new RuntimeException("Đã mở khóa rồi");
        }

        unlocked.add(itemId);
        user.setUnlockedItems(String.join(",", unlocked));
        user.addPoints(-price);

        // FIX: Phải save user sau khi thay đổi points và unlocked
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getNickname());
        return toAuthResponse(user, token);
    }

    // FIX: Thêm endpoint addPoints để frontend gọi khi hoàn thành mission
    @Transactional
    public AuthResponse addPoints(String nickname, long amount) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        user.addPoints(amount);
        userRepository.save(user); // ← Lưu vào DB
        String token = jwtUtil.generateToken(user.getNickname());
        return toAuthResponse(user, token);
    }

    // FIX: Sync streak từ client lên server
    @Transactional
    public AuthResponse syncStreak(String nickname, List<String> streakDates) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Merge streak client + server, giữ unique và sort
        String raw = user.getStreakDates() == null ? "" : user.getStreakDates();
        List<String> serverDates = raw.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(raw.split(",")));

        for (String d : streakDates) {
            if (!serverDates.contains(d)) serverDates.add(d);
        }

        // Chỉ giữ 7 ngày gần nhất
        serverDates.sort(String::compareTo);
        if (serverDates.size() > 7) {
            serverDates = serverDates.subList(serverDates.size() - 7, serverDates.size());
        }

        user.setStreakDates(String.join(",", serverDates));
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getNickname());
        return toAuthResponse(user, token);
    }

    // Helper: cập nhật streak hôm nay vào DB
    private void _updateStreakOnServer(User user) {
        String today = LocalDate.now().toString();
        String raw = user.getStreakDates() == null ? "" : user.getStreakDates();
        List<String> dates = raw.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(raw.split(",")));
        if (!dates.contains(today)) {
            dates.add(today);
            if (dates.size() > 7) dates = dates.subList(dates.size() - 7, dates.size());
            user.setStreakDates(String.join(",", dates));
        }
    }

    // Helper: build AuthResponse chuẩn
    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(
                user.getId(),
                user.getNickname(),
                token,
                user.getPoints(),
                user.getUnlockedItems()
        );
    }
}