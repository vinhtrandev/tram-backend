package com.vinhtran.tram.repository;

import com.vinhtran.tram.entity.Star;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface StarRepository extends JpaRepository<Star, Long> {

    /**
     * Xóa sao hết TTL theo type và createdAt.
     * Gọi SAU khi đã xóa reactions liên quan (FK constraint).
     */
    @Modifying
    @Query("DELETE FROM Star s WHERE s.type = :type AND s.createdAt < :cutoff")
    int deleteByTypeAndCreatedAtBefore(@Param("type") String type,
                                       @Param("cutoff") Instant cutoff);

    // FIX: bỏ deleteReactionsByExpiredStars() — không phải responsibility của StarRepository
    // Query đó đã được chuyển sang ReactionRepository.deleteByStarTypeAndCreatedAtBefore()
}