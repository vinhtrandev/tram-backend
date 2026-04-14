// com/vinhtran/tram/controller/TransactionController.java
package com.vinhtran.tram.controller;

import com.vinhtran.tram.entity.Transaction;
import com.vinhtran.tram.entity.User;
import com.vinhtran.tram.repository.TransactionRepository;
import com.vinhtran.tram.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // GET /api/transactions — lấy lịch sử của user hiện tại
    @GetMapping
    public ResponseEntity<?> getHistory(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));

        List<Transaction> txs = transactionRepository
                .findTop50ByUserNicknameOrderByCreatedAtDesc(auth.getName());

        List<Map<String, Object>> result = txs.stream().map(tx -> Map.<String, Object>of(
                "id",     tx.getId(),
                "type",   tx.getType(),
                "amount", tx.getAmount(),
                "desc",   tx.getDescription(),
                "time",   tx.getCreatedAt().toString()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // POST /api/transactions — ghi 1 giao dịch mới
    @PostMapping
    public ResponseEntity<?> addTransaction(
            @RequestBody Map<String, String> body,
            Authentication auth) {

        if (auth == null) return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));

        String nickname = auth.getName();
        User user = userRepository.findByNickname(nickname).orElse(null);
        // Nếu không tìm thấy user → bỏ qua (offline mode)
        if (user == null) return ResponseEntity.ok(Map.of("ok", false, "message", "User not found"));

        String type   = body.getOrDefault("type", "earn");
        String desc   = body.getOrDefault("desc", "");
        int amount;
        try {
            amount = Integer.parseInt(body.getOrDefault("amount", "0").replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            amount = 0;
        }

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