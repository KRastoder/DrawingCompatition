package com.drawing_compatition.KRastoder.drawing;

import jakarta.validation.constraints.Size;

public record UpdateDrawingRequest(
        @Size(max = 2000, message = "Description must be at most 2000 characters") String description
) {
}
