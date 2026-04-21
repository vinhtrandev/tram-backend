package com.vinhtran.tram.controller;

import com.vinhtran.tram.entity.Transaction;
import com.vinhtran.tram.entity.User;
import com.vinhtran.tram.repository.TransactionRepository;
import com.vinhtran.tram.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(ZoneOffset.UTC);

    private static final List<String> VALID_TYPES = List.of("earn", "spend", "bonus");

    /** GET /api/transactions */
    @GetMapping
    public ResponseEntity<?> getHistory(Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));

        List<Transaction> txs = transactionRepository
                .findTop50ByUserNicknameOrderByCreatedAtDesc(auth.getName());

        // FIX: Map.of() ném NullPointerException nếu value null (type/desc có thể null)
        // Dùng HashMap thay thế để an toàn với null value
        List<Map<String, Object>> result = txs.stream().map(tx -> {
            String time = tx.getCreatedAt() != null
                    ? ISO_UTC.format(tx.getCreatedAt())
                    : ISO_UTC.format(Instant.now());

            Map<String, Object> row = new HashMap<>();
            row.put("id",     tx.getId());
            row.put("type",   tx.getType()        != null ? tx.getType()        : "earn");
            row.put("amount", tx.getAmount());
            row.put("desc",   tx.getDescription() != null ? tx.getDescription() : "");
            row.put("time",   time);
            return row;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /** POST /api/transactions */
    @PostMapping
    public ResponseEntity<?> addTransaction(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));

        User user = userRepository.findByNickname(auth.getName()).orElse(null);
        if (user == null)
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));

        String type = String.valueOf(body.getOrDefault("type", "earn")).trim();
        if (!VALID_TYPES.contains(type)) type = "earn";

        String desc = String.valueOf(body.getOrDefault("desc", "")).trim();
        if (desc.length() > 200) desc = desc.substring(0, 200);

        // FIX: parse an toàn cả Number và String
        int amount = 0;
        try {
            Object raw = body.get("amount");
            if (raw instanceof Number) {
                amount = ((Number) raw).intValue();
            } else if (raw != null) {
                amount = Integer.parseInt(
                        String.valueOf(raw).replaceAll("[^0-9\\-]", ""));
            }
        } catch (NumberFormatException ignored) {
            amount = 0;
        }

        // FIX: tránh ghi amount âm kiểu "spend" với số dương
        // Chuẩn hóa: spend luôn âm, earn/bonus luôn dương
        if (type.equals("spend") && amount > 0) amount = -amount;
        if (!type.equals("spend") && amount < 0) amount = Math.abs(amount);

        Transaction tx = Transaction.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .description(desc)
                .build();
        transactionRepository.save(tx);

        return ResponseEntity.ok(Map.of("ok", true));
    }
}