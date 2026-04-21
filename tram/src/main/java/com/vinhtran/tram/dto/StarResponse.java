package com.vinhtran.tram.dto;

import com.vinhtran.tram.entity.Star;
import lombok.Data;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
public class StarResponse {

    private Long id;
    private String text;
    private String type;
    private Double x;
    private Double y;
    private Boolean moodPost;
    private Boolean negative;
    private String nickname;
    private Long userId;
    private Boolean isOwn;
    private Instant createdAt;
    private Instant expiredAt;

    private int listenCount;
    private int hugCount;
    private int strongCount;
    private int treasureCount;
    private int feelCount;
    private int thanksCount;

    private Set<String> myReactions = new HashSet<>();

    /**
     * FIX: isOwn không được set trong from() — phải set ở tầng Service
     * vì from() không biết currentUserId là ai.
     * from() chỉ map các field tĩnh từ entity.
     *
     * FIX: Dùng star.getUserId() (đã là @Transient) — đảm bảo không lazy-load author
     * ngoài transaction. Nếu author là null thì userId = null (an toàn).
     */
    public static StarResponse from(Star star) {
        StarResponse r = new StarResponse();
        r.setId(star.getId());
        r.setText(star.getText());
        r.setType(star.getType());
        r.setX(star.getX());
        r.setY(star.getY());
        r.setMoodPost(star.getMoodPost());
        r.setNegative(star.getNegative());
        r.setNickname(star.getNickname());
        r.setUserId(star.getUserId());   // @Transient helper — null-safe
        r.setCreatedAt(star.getCreatedAt());
        r.setExpiredAt(star.getExpiredAt());
        r.setListenCount(star.getListenCount());
        r.setHugCount(star.getHugCount());
        r.setStrongCount(star.getStrongCount());
        r.setTreasureCount(star.getTreasureCount());
        r.setFeelCount(star.getFeelCount());
        r.setThanksCount(star.getThanksCount());
        // isOwn = null ở đây; Service sẽ setIsOwn() sau khi biết currentUserId
        r.setIsOwn(false);
        return r;
    }
}