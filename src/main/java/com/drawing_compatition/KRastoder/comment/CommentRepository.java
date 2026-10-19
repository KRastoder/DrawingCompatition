package com.drawing_compatition.KRastoder.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByDrawingIdOrderByCreatedAtAsc(Long drawingId);
}
