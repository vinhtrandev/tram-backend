package com.vinhtran.tram.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "stars", indexes = {
        @Index(name = "idx_stars_type_created", columnList = "type, created_at")
})
@Getter
@Setter
public class Star {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(length = 20)
    private String type;

    private Double x;
    private Double y;

    @Column(nullable = false)
    private Boolean moodPost = false;

    @Column(nullable = false)
    private Boolean negative = false;

    @Column(nullable = false)
    private Boolean haloEffect = false;

    @Column(nullable = false)
    private Boolean tailEffect = false;

    @Column(nullable = false)
    private Double size = 4.0;

    @Column(nullable = false)
    private Double opacity = 0.85;

    @Column(length = 50)
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant createdAt;

    @Column(name = "expired_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant expiredAt;

    @Column(nullable = false)
    private int listenCount   = 0;
    @Column(nullable = false)
    private int hugCount      = 0;
    @Column(nullable = false)
    private int strongCount   = 0;
    @Column(nullable = false)
    private int treasureCount = 0;
    @Column(nullable = false)
    private int feelCount     = 0;
    @Column(nullable = false)
    private int thanksCount   = 0;

    @PrePersist
    protected void onCreate() {
        if (createdAt  == null) createdAt  = Instant.now();
        if (moodPost   == null) moodPost   = false;
        if (negative   == null) negative   = false;
        if (haloEffect == null) haloEffect = false;
        if (tailEffect == null) tailEffect = false;
        if (size       == null) size       = 4.0;
        if (opacity    == null) opacity    = 0.85;
    }

    @Transient
    public Long getUserId() {
        return author != null ? author.getId() : null;
    }
}