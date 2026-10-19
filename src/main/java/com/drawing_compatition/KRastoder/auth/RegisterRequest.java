package com.drawing_compatition.KRastoder.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Pattern(
                regexp = "^[a-zA-Z0-9_.-]{3,30}$",
                message = "Username must be 3-30 characters and may contain letters, numbers, dots, dashes, and underscores"
        )
        String username,

        @NotBlank(message = "Display name is required")
        @Size(max = 50, message = "Display name must be at most 50 characters")
        String displayName,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {
}
