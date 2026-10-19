package com.drawing_compatition.KRastoder.comment;

import com.drawing_compatition.KRastoder.common.BadRequestException;
import com.drawing_compatition.KRastoder.common.ForbiddenException;
import com.drawing_compatition.KRastoder.common.NotFoundException;
import com.drawing_compatition.KRastoder.drawing.Drawing;
import com.drawing_compatition.KRastoder.drawing.DrawingRepository;
import com.drawing_compatition.KRastoder.user.User;
import com.drawing_compatition.KRastoder.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository comments;
    private final DrawingRepository drawings;
    private final UserRepository users;

    public CommentService(CommentRepository comments, DrawingRepository drawings, UserRepository users) {
        this.comments = comments;
        this.drawings = drawings;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long drawingId) {
        if (!drawings.existsById(drawingId)) {
            throw new NotFoundException("Drawing not found.");
        }

        return comments.findByDrawingIdOrderByCreatedAtAsc(drawingId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentResponse create(Long drawingId, Long userId, CommentRequest request) {
        if (!drawings.existsById(drawingId)) {
            throw new NotFoundException("Drawing not found.");
        }

        Drawing drawing = drawings.getReferenceById(drawingId);
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));

        Comment comment = comments.save(new Comment(drawing, user, normalizeBody(request.body())));
        return CommentResponse.from(comment);
    }

    @Transactional
    public CommentResponse update(Long commentId, Long userId, CommentRequest request) {
        Comment comment = ownedComment(commentId, userId);
        comment.setBody(normalizeBody(request.body()));
        comments.save(comment);
        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long commentId, Long userId) {
        comments.delete(ownedComment(commentId, userId));
    }

    private Comment ownedComment(Long commentId, Long userId) {
        Comment comment = comments.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found."));

        if (!comment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only change your own comments.");
        }

        return comment;
    }

    private String normalizeBody(String body) {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Comment cannot be empty.");
        }
        if (trimmed.length() > 1000) {
            throw new BadRequestException("Comment must be at most 1000 characters.");
        }
        return trimmed;
    }
}
