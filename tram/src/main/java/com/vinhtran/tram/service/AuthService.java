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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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
        return new AuthResponse(user.getId(), user.getNickname(), token, user.getPoints());
    }

    @Transactional
    public AuthResponse login(AuthRequest req) {
        User user = userRepository.findByNickname(req.getNickname())
                .orElseThrow(() -> new RuntimeException("Sai mật danh hoặc chìa khóa"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Sai mật danh hoặc chìa khóa");
        }
        user.setLastLogin(java.time.LocalDateTime.now());
        // ✅ không cần gọi save() riêng vì đã trong @Transactional
        // Hibernate tự dirty-check và update
        String token = jwtUtil.generateToken(user.getNickname());
        return new AuthResponse(user.getId(), user.getNickname(), token, user.getPoints());
    }
}