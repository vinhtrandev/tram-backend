package com.vinhtran.tram.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /**
     * FIX circular dependency:
     * JwtFilter → UserDetailsService (bean trong SecurityConfig)
     *           → SecurityConfig → JwtFilter  ← vòng tròn!
     *
     * @Lazy khiến Spring inject proxy thay vì bean thật ngay lúc khởi tạo,
     * phá vỡ vòng tròn mà không cần tách class mới hay allow-circular-references.
     * KHÔNG dùng @RequiredArgsConstructor vì Lombok không hỗ trợ @Lazy trên param.
     */
    @Autowired
    public JwtFilter(JwtUtil jwtUtil, @Lazy UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String header = req.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();

            // FIX: kiểm tra token không rỗng trước khi validate
            if (!token.isBlank() && jwtUtil.validate(token)) {
                String nickname = jwtUtil.getNickname(token);

                // FIX: chỉ set auth nếu chưa có (tránh override auth đã set bởi filter khác)
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(nickname);
                    var auth = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        chain.doFilter(req, res);
    }
}