package com.drawing_compatition.KRastoder.drawing;

import java.time.Instant;

public record DrawingResponse(
        Long id,
        String description,
        Instant createdAt,
        UserSummary owner,
        long voteCount,
        boolean votedByMe,
        String imageUrl
) {

    public record UserSummary(Long id, String username, String displayName) {
    }

    public static DrawingResponse from(DrawingSummary summary) {
        return new DrawingResponse(
                summary.id(),
                summary.description(),
                summary.createdAt(),
                new UserSummary(summary.ownerId(), summary.ownerUsername(), summary.ownerDisplayName()),
                summary.voteCount(),
                summary.votedByMe() > 0,
                "/api/drawings/" + summary.id() + "/image"
        );
    }
}
