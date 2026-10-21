package com.drawing_compatition.KRastoder.user;

import com.drawing_compatition.KRastoder.common.BadRequestException;
import com.drawing_compatition.KRastoder.common.NotFoundException;
import com.drawing_compatition.KRastoder.common.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    static final int MAX_BIO_LENGTH = 500;
    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private final UserRepository users;
    private final StorageService storage;

    public ProfileService(UserRepository users, StorageService storage) {
        this.users = users;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id", unless = "#result == null")
    public UserResponse get(Long id) {
        log.debug("Getting profile for user {}", id);
        try {
            return UserResponse.from(find(id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting profile for user {}", id, e);
            throw new RuntimeException("Failed to get profile", e);
        }
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserResponse updateBio(Long id, String rawBio) {
        log.debug("Updating bio for user {}", id);
        try {
            User user = find(id);
            user.setBio(normalizeBio(rawBio));
            users.save(user);
            log.info("Updated bio for user {}", id);
            return UserResponse.from(user);
        } catch (NotFoundException | BadRequestException e) {
            log.warn("Failed to update bio for user {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating bio for user {}", id, e);
            throw new RuntimeException("Failed to update bio", e);
        }
    }

    @Transactional
    @CacheEvict(value = {"users", "avatars"}, key = "#id")
    public UserResponse updateAvatar(Long id, MultipartFile image) {
        log.debug("Updating avatar for user {}", id);
        try {
            User user = find(id);
            ValidatedImage validatedImage = validateImage(image);

            // Delete old avatar if exists
            if (user.isHasAvatar()) {
                try {
                    storage.delete(user.getAvatarStorageKey());
                } catch (Exception e) {
                    log.warn("Failed to delete old avatar for user {}: {}", id, e.getMessage());
                }
            }

            // Store new avatar
            String storageKey = storage.store(validatedImage.contentType(), validatedImage.data());
            user.setAvatar(storageKey, validatedImage.contentType(), validatedImage.data().length);
            users.save(user);
            log.info("Updated avatar for user {}", id);
            return UserResponse.from(user);
        } catch (NotFoundException | BadRequestException e) {
            log.warn("Failed to update avatar for user {}: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating avatar for user {}", id, e);
            throw new RuntimeException("Failed to update avatar", e);
        }
    }

    @Transactional
    @CacheEvict(value = {"users", "avatars"}, key = "#id")
    public UserResponse removeAvatar(Long id) {
        log.debug("Removing avatar for user {}", id);
        try {
            User user = find(id);
            if (user.isHasAvatar()) {
                try {
                    storage.delete(user.getAvatarStorageKey());
                } catch (Exception e) {
                    log.warn("Failed to delete avatar for user {}: {}", id, e.getMessage());
                }
                user.clearAvatar();
                users.save(user);
            }
            log.info("Removed avatar for user {}", id);
            return UserResponse.from(user);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error removing avatar for user {}", id, e);
            throw new RuntimeException("Failed to remove avatar", e);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "avatars", key = "#id", unless = "#result == null")
    public StorageService.StoredObject getAvatar(Long id) {
        log.debug("Getting avatar for user {}", id);
        try {
            User user = find(id);
            if (!user.isHasAvatar()) {
                throw new NotFoundException("This user does not have a profile picture.");
            }
            return storage.retrieve(user.getAvatarStorageKey())
                    .orElseThrow(() -> new NotFoundException("Avatar not found in storage."));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting avatar for user {}", id, e);
            throw new RuntimeException("Failed to get avatar", e);
        }
    }

    private User find(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found."));
    }

    private String normalizeBio(String bio) {
        if (bio == null || bio.isBlank()) {
            return null;
        }
        String trimmed = bio.trim();
        if (trimmed.length() > MAX_BIO_LENGTH) {
            throw new BadRequestException("The description must be at most 500 characters.");
        }
        return trimmed;
    }

    private ValidatedImage validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("Choose a profile picture to upload.");
        }

        if (image.getSize() > MAX_AVATAR_BYTES) {
            throw new BadRequestException("The profile picture must be 2 MB or smaller.");
        }

        String contentType = image.getContentType();
        if (contentType == null) {
            throw new BadRequestException("The profile picture type is missing.");
        }

        String normalizedType = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedType)) {
            throw new BadRequestException("Only PNG, JPEG, and WebP images are supported.");
        }

        try {
            return new ValidatedImage(image.getBytes(), normalizedType);
        } catch (IOException exception) {
            throw new BadRequestException("The profile picture could not be read.");
        }
    }

    private record ValidatedImage(byte[] data, String contentType) {
    }
}
