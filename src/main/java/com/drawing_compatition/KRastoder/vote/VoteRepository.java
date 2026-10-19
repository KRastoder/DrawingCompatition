package com.drawing_compatition.KRastoder.vote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByDrawingIdAndUserId(Long drawingId, Long userId);

    Optional<Vote> findByDrawingIdAndUserId(Long drawingId, Long userId);
}
