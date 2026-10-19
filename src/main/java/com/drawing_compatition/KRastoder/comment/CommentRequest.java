package com.drawing_compatition.KRastoder.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Comment cannot be empty")
        @Size(max = 1000, message = "Comment must be at most 1000 characters")
        String body
) {
}
