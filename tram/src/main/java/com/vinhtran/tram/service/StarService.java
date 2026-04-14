package com.vinhtran.tram.service;

import com.vinhtran.tram.dto.StarRequest;
import com.vinhtran.tram.dto.StarResponse;
import com.vinhtran.tram.entity.Reaction;
import com.vinhtran.tram.entity.Star;
import com.vinhtran.tram.entity.User;
import com.vinhtran.tram.repository.ReactionRepository;
import com.vinhtran.tram.repository.StarRepository;
import com.vinhtran.tram.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StarService {

    private final StarRepository starRepository;
    private final UserRepository userRepository;
    private final ReactionRepository reactionRepository;

    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "mệt", "buồn", "khóc", "cô đơn", "chán", "tệ", "sợ", "lo",
            "áp lực", "stress", "đau", "thất bại", "tuyệt vọng"
    );

    @Transactional(readOnly = true)
    public List<StarResponse> getAllStars() {
        return starRepository.findTop200ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StarResponse createStar(StarRequest req, String nickname) {
        User author = (nickname != null && !nickname.isBlank())
                ? userRepository.findByNickname(nickname).orElse(null)
                : null;

        boolean negative = NEGATIVE_KEYWORDS.stream()
                .anyMatch(k -> req.getText().toLowerCase().contains(k));

        boolean hasTail = false;
        boolean hasHalo = false;
        if (author != null && author.getUnlockedItems() != null) {
            hasTail = author.getUnlockedItems().contains("trail_star");
            hasHalo = author.getUnlockedItems().contains("halo_star");
        }

        Star star = Star.builder()
                .text(req.getText())
                .type(req.getType() != null ? req.getType() : "normal")
                .x(req.getX())
                .y(req.getY())
                .size(3 + Math.random() * 5)
                .opacity(0.6 + Math.random() * 0.4)
                .negative(negative)
                .tailEffect(hasTail)
                .haloEffect(hasHalo)
                .author(author)
                .build();

        star = starRepository.save(star);

        if (author != null) {
            author.addPoints(5);
            userRepository.save(author);
        }

        return toResponse(star);
    }

    @Transactional
    public void addReaction(Long starId, String type, String senderNickname) {
        Star star = starRepository.findById(starId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sao"));

        User sender = (senderNickname != null && !senderNickname.isBlank())
                ? userRepository.findByNickname(senderNickname).orElse(null)
                : null;

        Reaction reaction = Reaction.builder()
                .type(type)
                .star(star)
                .sender(sender)
                .build();
        reactionRepository.save(reaction);

        switch (type) {
            case "listen" -> star.setListenCount(star.getListenCount() + 1);
            case "hug"    -> star.setHugCount(star.getHugCount() + 1);
            case "strong" -> star.setStrongCount(star.getStrongCount() + 1);
        }

        if (star.getAuthor() != null) {
            star.getAuthor().addPoints(3);
            userRepository.save(star.getAuthor());
        }
    }

    private StarResponse toResponse(Star s) {
        StarResponse r = new StarResponse();
        r.setId(s.getId());
        r.setText(s.getText());
        r.setType(s.getType());
        r.setX(s.getX());
        r.setY(s.getY());
        r.setSize(s.getSize());
        r.setOpacity(s.getOpacity());
        r.setNegative(s.isNegative());
        r.setTailEffect(s.isTailEffect());
        r.setHaloEffect(s.isHaloEffect());
        r.setListenCount(s.getListenCount());
        r.setHugCount(s.getHugCount());
        r.setStrongCount(s.getStrongCount());
        r.setCreatedAt(s.getCreatedAt() != null
                ? s.getCreatedAt().toString()
                : null);
        r.setNickname(s.getAuthor() != null ? s.getAuthor().getNickname() : "Ẩn danh");
        return r;
    }
}