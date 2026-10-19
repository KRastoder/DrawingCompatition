package com.drawing_compatition.KRastoder.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = ProfileService.MAX_BIO_LENGTH, message = "Description must be at most 500 characters.")
        String bio
) {
}
