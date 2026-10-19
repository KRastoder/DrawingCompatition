package com.drawing_compatition.KRastoder.user;

import com.drawing_compatition.KRastoder.auth.AppUserPrincipal;

public record UserResponse(Long id, String username, String displayName) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName());
    }

    public static UserResponse from(AppUserPrincipal principal) {
        return new UserResponse(principal.id(), principal.getUsername(), principal.displayName());
    }
}
