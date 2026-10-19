package com.drawing_compatition.KRastoder.user;

import com.drawing_compatition.KRastoder.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/profile")
    public UserResponse ownProfile(@AuthenticationPrincipal AppUserPrincipal principal) {
        return profiles.get(principal.id());
    }

    @GetMapping("/users/{id}")
    public UserResponse userProfile(@PathVariable Long id) {
        return profiles.get(id);
    }

    @PatchMapping("/profile")
    public UserResponse updateBio(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return profiles.updateBio(principal.id(), request.bio());
    }

    @PutMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse updateAvatar(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestPart("image") MultipartFile image
    ) {
        return profiles.updateAvatar(principal.id(), image);
    }

    @DeleteMapping("/profile/avatar")
    public UserResponse removeAvatar(@AuthenticationPrincipal AppUserPrincipal principal) {
        return profiles.removeAvatar(principal.id());
    }

    @GetMapping("/users/{id}/avatar")
    public ResponseEntity<byte[]> avatar(@PathVariable Long id) {
        ProfileService.Avatar avatar = profiles.getAvatar(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(avatar.contentType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .body(avatar.data());
    }
}
