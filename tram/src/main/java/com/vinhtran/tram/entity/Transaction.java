package com.vinhtran.tram.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 200)
    private String description;

    /**
     * Dùng @Column(columnDefinition) để ép Hibernate luôn đọc/ghi UTC.
     * DB lưu "timestamp without time zone" → Hibernate mặc định hiểu là
     * giờ JVM (có thể lệch nếu server không set timezone). Fix: luôn
     * format/parse rõ ràng qua Instant + toString() có "Z".
     */
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}