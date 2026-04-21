package com.vinhtran.tram.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String nickname;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private long points = 0;

    /* FIX: dùng Instant (UTC) thay LocalDateTime → không bị lệch timezone */
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant createdAt;

    @Column(name = "last_login",
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant lastLogin;

    @Column(name = "streak_dates", columnDefinition = "TEXT")
    @Builder.Default
    private String streakDates = "";

    @Column(name = "unlocked_items", columnDefinition = "TEXT")
    @Builder.Default
    private String unlockedItems = "";

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Star> stars = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void addPoints(long amount) {
        this.points = Math.max(0, this.points + amount);
    }
}