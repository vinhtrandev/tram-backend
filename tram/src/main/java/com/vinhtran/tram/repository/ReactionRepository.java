package com.vinhtran.tram.repository;

import com.vinhtran.tram.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
}