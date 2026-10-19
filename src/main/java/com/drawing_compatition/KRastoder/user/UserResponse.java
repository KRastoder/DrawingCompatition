package com.drawing_compatition.KRastoder.user;

public record UserResponse(Long id, String username, String displayName, String bio, String avatarUrl) {

    public static UserResponse from(User user) {
        String avatarUrl = user.isHasAvatar() ? "/api/users/" + user.getId() + "/avatar" : null;
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                avatarUrl
        );
    }
}
