package com.drawing_compatition.KRastoder.comment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String body,
        Instant createdAt,
        Instant updatedAt,
        Author author
) {

    public record Author(Long id, String username, String displayName, String avatarUrl) {
    }

    public static CommentResponse from(Comment comment) {
        String avatarUrl = comment.getUser().isHasAvatar()
                ? "/api/users/" + comment.getUser().getId() + "/avatar"
                : null;
        return new CommentResponse(
                comment.getId(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                new Author(
                        comment.getUser().getId(),
                        comment.getUser().getUsername(),
                        comment.getUser().getDisplayName(),
                        avatarUrl
                )
        );
    }
}
