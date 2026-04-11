package com.vinhtran.tram.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stars")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Star {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String text;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String type = "normal";

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(nullable = false)
    @Builder.Default
    private double size = 4.0;

    @Column(nullable = false)
    @Builder.Default
    private double opacity = 0.8;

    @Column(nullable = false)
    @Builder.Default
    private boolean negative = false;

    @Column(name = "tail_effect", nullable = false)
    @Builder.Default
    private boolean tailEffect = false;

    @Column(name = "halo_effect", nullable = false)
    @Builder.Default
    private boolean haloEffect = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @OneToMany(mappedBy = "star", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reaction> reactions = new ArrayList<>();

    @Column(name = "listen_count", nullable = false)
    @Builder.Default
    private int listenCount = 0;

    @Column(name = "hug_count", nullable = false)
    @Builder.Default
    private int hugCount = 0;

    @Column(name = "strong_count", nullable = false)
    @Builder.Default
    private int strongCount = 0;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}