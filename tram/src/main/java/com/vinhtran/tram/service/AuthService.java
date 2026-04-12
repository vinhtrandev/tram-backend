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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ✅ ID khớp với config.js
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
        return new AuthResponse(user.getId(), user.getNickname(), token, user.getPoints(), user.getUnlockedItems());
    }

    @Transactional
    public AuthResponse login(AuthRequest req) {
        User user = userRepository.findByNickname(req.getNickname())
                .orElseThrow(() -> new RuntimeException("Sai mật danh hoặc chìa khóa"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Sai mật danh hoặc chìa khóa");
        }
        user.setLastLogin(java.time.LocalDateTime.now());
        String token = jwtUtil.generateToken(user.getNickname());
        return new AuthResponse(user.getId(), user.getNickname(), token, user.getPoints(), user.getUnlockedItems());
    }

    @Transactional(readOnly = true)
    public AuthResponse getMe(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        String token = jwtUtil.generateToken(user.getNickname());
        return new AuthResponse(user.getId(), user.getNickname(), token, user.getPoints(), user.getUnlockedItems());
    }

    @Transactional
    public AuthResponse unlockItem(String nickname, String itemId) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // ✅ Lấy giá từ server, không tin client
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

        String token = jwtUtil.generateToken(user.getNickname());
        return new AuthResponse(user.getId(), user.getNickname(), token, user.getPoints(), user.getUnlockedItems());
    }
}