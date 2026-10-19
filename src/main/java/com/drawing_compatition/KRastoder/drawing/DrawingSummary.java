package com.drawing_compatition.KRastoder.drawing;

import java.time.Instant;

public record DrawingSummary(
        Long id,
        String description,
        Instant createdAt,
        Long ownerId,
        String ownerUsername,
        String ownerDisplayName,
        long voteCount,
        long votedByMe,
        boolean ownerHasAvatar
) {
}
