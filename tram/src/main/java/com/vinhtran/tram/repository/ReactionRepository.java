package com.vinhtran.tram.repository;

import com.vinhtran.tram.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByStarIdAndSenderIdAndType(Long starId, Long senderId, String type);

    List<Reaction> findAllByStarIdAndSenderId(Long starId, Long senderId);

    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.star.id = :starId AND r.sender.id = :senderId AND r.type = :type")
    int deleteByStarIdAndSenderIdAndType(@Param("starId") Long starId,
                                         @Param("senderId") Long senderId,
                                         @Param("type") String type);

    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.star.id = :starId")
    void deleteByStarId(@Param("starId") Long starId);

    /**
     * FIX: StarService gọi starRepository.deleteReactionsByExpiredStars() — method không tồn tại.
     * Chuyển query đúng vào đây để cleanup scheduled hoạt động.
     * Xóa tất cả reactions thuộc các star theo type và createdAt trước cutoff.
     */
    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.star.type = :type AND r.star.createdAt < :cutoff")
    void deleteByStarTypeAndCreatedAtBefore(@Param("type") String type,
                                            @Param("cutoff") Instant cutoff);
}