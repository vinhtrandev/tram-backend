package com.vinhtran.tram.repository;

import com.vinhtran.tram.entity.Star;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StarRepository extends JpaRepository<Star, Long> {
    List<Star> findTop200ByOrderByCreatedAtDesc();
}