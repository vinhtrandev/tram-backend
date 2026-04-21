package com.vinhtran.tram.service;

import com.vinhtran.tram.dto.StarRequest;
import com.vinhtran.tram.dto.StarResponse;
import com.vinhtran.tram.entity.Reaction;
import com.vinhtran.tram.entity.User;
import com.vinhtran.tram.repository.ReactionRepository;
import com.vinhtran.tram.repository.StarRepository;
import com.vinhtran.tram.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.vinhtran.tram.entity.Star;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StarService {

    private final StarRepository     starRepository;
    private final UserRepository     userRepository;
    private final ReactionRepository reactionRepository;

    private static final long TTL_SHOOTING_H = 4L;
    private static final long TTL_CLUSTER_H  = 24L;

    private static final Set<String> VALID_REACTION_TYPES =
            Set.of("listen", "hug", "strong", "treasure", "feel", "thanks");

    private Long getTtlHours(String type) {
        if (type == null) return null;
        return switch (type) {
            case "shooting" -> TTL_SHOOTING_H;
            case "cluster"  -> TTL_CLUSTER_H;
            default         -> null;
        };
    }

    private boolean isExpired(Star star) {
        Long ttlH = getTtlHours(star.getType());
        if (ttlH == null || star.getCreatedAt() == null) return false;
        return Instant.now().isAfter(star.getCreatedAt().plus(ttlH, ChronoUnit.HOURS));
    }

    private User resolveUser(String nickname) {
        if (nickname == null || nickname.isBlank()) return null;
        return userRepository.findByNickname(nickname).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<StarResponse> getActiveStars(String currentNickname) {
        User currentUser   = resolveUser(currentNickname);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;

        return starRepository.findAll().stream()
                .filter(s -> !isExpired(s))
                .map(star -> buildResponse(star, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional
    public StarResponse createStar(StarRequest req, String nickname) {
        User author = resolveUser(nickname);

        Star star = new Star();
        star.setText(req.getText());
        star.setType(req.getType() != null ? req.getType() : "shooting");
        star.setX(req.getX());
        star.setY(req.getY());

        // FIX: dùng getMoodPost() — getter đúng của Boolean wrapper
        star.setMoodPost(req.getMoodPost() != null ? req.getMoodPost() : false);

        // FIX: set tất cả field NOT NULL còn thiếu — tránh null constraint violation
        star.setNegative(false);
        star.setHaloEffect(false);
        star.setTailEffect(false);
        star.setSize(req.getSize() != null ? req.getSize() : 4.0);
        star.setOpacity(req.getOpacity() != null ? req.getOpacity() : 0.85);

        // Counters — set rõ ràng cho chắc
        star.setListenCount(0);
        star.setHugCount(0);
        star.setStrongCount(0);
        star.setTreasureCount(0);
        star.setFeelCount(0);
        star.setThanksCount(0);

        star.setNickname(author != null ? author.getNickname() : req.getNickname());
        star.setAuthor(author);

        Instant now = Instant.now();
        star.setCreatedAt(now);

        Long ttlH = getTtlHours(star.getType());
        if (ttlH != null) {
            star.setExpiredAt(now.plus(ttlH, ChronoUnit.HOURS));
        }

        Star saved = starRepository.save(star);
        StarResponse resp = StarResponse.from(saved);
        resp.setIsOwn(true);
        return resp;
    }

    @Transactional
    public StarResponse addReaction(Long starId, String type, String nickname) {
        if (!VALID_REACTION_TYPES.contains(type))
            throw new RuntimeException("Loại reaction không hợp lệ: " + type);

        Star star = starRepository.findById(starId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sao"));

        User sender = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        boolean alreadyReacted = reactionRepository
                .findByStarIdAndSenderIdAndType(starId, sender.getId(), type)
                .isPresent();

        if (!alreadyReacted) {
            Reaction reaction = Reaction.builder()
                    .star(star)
                    .sender(sender)
                    .type(type)
                    .build();
            reactionRepository.save(reaction);
            updateCounter(star, type, +1);
            starRepository.save(star);
        }

        return buildResponse(star, sender.getId());
    }

    @Transactional
    public StarResponse removeReaction(Long starId, String type, String nickname) {
        if (!VALID_REACTION_TYPES.contains(type))
            throw new RuntimeException("Loại reaction không hợp lệ: " + type);

        Star star = starRepository.findById(starId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sao"));

        User sender = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        int deleted = reactionRepository
                .deleteByStarIdAndSenderIdAndType(starId, sender.getId(), type);

        if (deleted > 0) {
            updateCounter(star, type, -1);
            starRepository.save(star);
        }

        return buildResponse(star, sender.getId());
    }

    private void updateCounter(Star star, String type, int delta) {
        switch (type) {
            case "listen"   -> star.setListenCount(Math.max(0, star.getListenCount()     + delta));
            case "hug"      -> star.setHugCount(Math.max(0, star.getHugCount()           + delta));
            case "strong"   -> star.setStrongCount(Math.max(0, star.getStrongCount()     + delta));
            case "treasure" -> star.setTreasureCount(Math.max(0, star.getTreasureCount() + delta));
            case "feel"     -> star.setFeelCount(Math.max(0, star.getFeelCount()         + delta));
            case "thanks"   -> star.setThanksCount(Math.max(0, star.getThanksCount()     + delta));
        }
    }

    private StarResponse buildResponse(Star star, Long currentUserId) {
        StarResponse resp = StarResponse.from(star);
        if (currentUserId != null) {
            Set<String> myReacted = reactionRepository
                    .findAllByStarIdAndSenderId(star.getId(), currentUserId)
                    .stream()
                    .map(Reaction::getType)
                    .collect(Collectors.toSet());
            resp.setMyReactions(myReacted);
            resp.setIsOwn(currentUserId.equals(resp.getUserId()));
        }
        return resp;
    }

    @Transactional
    public void deleteIfExpired(Long id) {
        starRepository.findById(id).ifPresent(star -> {
            if (isExpired(star)) {
                reactionRepository.deleteByStarId(id);
                starRepository.delete(star);
                log.debug("⭐ Xóa sao hết TTL [id={}] type={}", id, star.getType());
            }
        });
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    @Transactional
    public void cleanupExpiredStars() {
        Instant now            = Instant.now();
        Instant cutoffShooting = now.minus(TTL_SHOOTING_H, ChronoUnit.HOURS);
        Instant cutoffCluster  = now.minus(TTL_CLUSTER_H,  ChronoUnit.HOURS);

        reactionRepository.deleteByStarTypeAndCreatedAtBefore("shooting", cutoffShooting);
        int ds = starRepository.deleteByTypeAndCreatedAtBefore("shooting", cutoffShooting);

        reactionRepository.deleteByStarTypeAndCreatedAtBefore("cluster", cutoffCluster);
        int dc = starRepository.deleteByTypeAndCreatedAtBefore("cluster", cutoffCluster);

        if (ds > 0 || dc > 0)
            log.info("🌌 Dọn sao hết hạn: {} sao băng, {} chùm sao", ds, dc);
    }
}