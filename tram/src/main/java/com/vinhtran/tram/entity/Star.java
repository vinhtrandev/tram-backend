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

    @Column(nullable = false)
    @Builder.Default
    private boolean tailEffect = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean haloEffect = false;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @OneToMany(mappedBy = "star", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reaction> reactions = new ArrayList<>();

    @Column(nullable = false) @Builder.Default private int listenCount = 0;
    @Column(nullable = false) @Builder.Default private int hugCount    = 0;
    @Column(nullable = false) @Builder.Default private int strongCount = 0;
}