package com.drawing_compatition.KRastoder.comment;

import com.drawing_compatition.KRastoder.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/drawings/{drawingId}/comments")
    public List<CommentResponse> list(@PathVariable Long drawingId) {
        return commentService.list(drawingId);
    }

    @PostMapping("/drawings/{drawingId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long drawingId,
            @Valid @RequestBody CommentRequest request
    ) {
        return commentService.create(drawingId, principal.id(), request);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentResponse update(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request
    ) {
        return commentService.update(commentId, principal.id(), request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long commentId
    ) {
        commentService.delete(commentId, principal.id());
    }
}
