package com.drawing_compatition.KRastoder.drawing;

import com.drawing_compatition.KRastoder.common.BadRequestException;
import com.drawing_compatition.KRastoder.common.ForbiddenException;
import com.drawing_compatition.KRastoder.common.NotFoundException;
import com.drawing_compatition.KRastoder.common.StorageService;
import com.drawing_compatition.KRastoder.user.User;
import com.drawing_compatition.KRastoder.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DrawingService {

    private static final Logger log = LoggerFactory.getLogger(DrawingService.class);

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private final DrawingRepository drawings;
    private final UserRepository users;
    private final StorageService storage;

    public DrawingService(DrawingRepository drawings, UserRepository users, StorageService storage) {
        this.drawings = drawings;
        this.users = users;
        this.storage = storage;
    }

    @Transactional
    @CacheEvict(value = "drawings", allEntries = true)
    public DrawingResponse create(Long userId, String rawDescription, MultipartFile image) {
        log.debug("Creating drawing for userId: {}", userId);
        try {
            User user = users.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found."));

            String description = normalizeDescription(rawDescription);
            ValidatedImage validatedImage = validateImage(image);

            // Store image via StorageService
            String storageKey = storage.store(validatedImage.contentType(), validatedImage.data());

            Drawing drawing = drawings.save(new Drawing(
                    user,
                    description,
                    storageKey,
                    validatedImage.contentType(),
                    validatedImage.data().length
            ));

            log.info("Created drawing {} for user {}", drawing.getId(), userId);

            DrawingSummary summary = new DrawingSummary(
                    drawing.getId(),
                    drawing.getDescription(),
                    drawing.getCreatedAt(),
                    user.getId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    0,
                    0,
                    user.isHasAvatar()
            );
            return DrawingResponse.from(summary);
        } catch (NotFoundException | BadRequestException e) {
            log.warn("Failed to create drawing for user {}: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating drawing for user {}", userId, e);
            throw new RuntimeException("Failed to create drawing", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DrawingResponse> list(Long viewerId, LocalDate date, LocalDate from, LocalDate to) {
        return listPage(viewerId, date, from, to, 0, Integer.MAX_VALUE).getContent();
    }

    @Transactional(readOnly = true)
    public Page<DrawingResponse> listPage(Long viewerId, LocalDate date, LocalDate from, LocalDate to, int page, int size) {
        log.debug("Listing drawings page {} size {} for viewer {}", page, size, viewerId);
        try {
            DateRange range = DateRange.of(date, from, to);
            // Use database-level pagination with sorting
            Page<DrawingSummary> summaryPage = drawings.findSummaries(range.from(), range.to(), viewerId, PageRequest.of(page, size));

            boolean hasDateFilter = date != null || from != null;

            if (summaryPage.isEmpty() && hasDateFilter && page == 0) {
                throw new NotFoundException(emptyMessage(date, from, to));
            }

            return new PageImpl<>(
                    summaryPage.getContent().stream().map(DrawingResponse::from).toList(),
                    summaryPage.getPageable(),
                    summaryPage.getTotalElements()
            );
        } catch (NotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing drawings for viewer {}", viewerId, e);
            throw new RuntimeException("Failed to list drawings", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DrawingResponse> listMine(Long userId) {
        return listMinePage(userId, 0, Integer.MAX_VALUE).getContent();
    }

    @Transactional(readOnly = true)
    public Page<DrawingResponse> listMinePage(Long userId, int page, int size) {
        log.debug("Listing user {} drawings page {} size {}", userId, page, size);
        try {
            // Use database-level pagination with sorting (createdAt desc, id desc)
            Page<DrawingSummary> summaryPage = drawings.findSummariesByOwner(userId, userId, PageRequest.of(page, size));

            return new PageImpl<>(
                    summaryPage.getContent().stream().map(DrawingResponse::from).toList(),
                    summaryPage.getPageable(),
                    summaryPage.getTotalElements()
            );
        } catch (Exception e) {
            log.error("Error listing drawings for user {}", userId, e);
            throw new RuntimeException("Failed to list user drawings", e);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "drawings", key = "#id + '_' + #viewerId", unless = "#result == null")
    public DrawingResponse get(Long id, Long viewerId) {
        log.debug("Getting drawing {} for viewer {}", id, viewerId);
        try {
            return drawings.findSummaryById(id, viewerId)
                    .map(DrawingResponse::from)
                    .orElseThrow(() -> new NotFoundException("Drawing not found."));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting drawing {}", id, e);
            throw new RuntimeException("Failed to get drawing", e);
        }
    }

    @Transactional(readOnly = true)
    public StorageService.StoredObject getImage(Long id) {
        log.debug("Getting image for drawing {}", id);
        try {
            Drawing drawing = drawings.findById(id)
                    .orElseThrow(() -> new NotFoundException("Drawing not found."));
            return storage.retrieve(drawing.getStorageKey())
                    .orElseThrow(() -> new NotFoundException("Drawing image not found in storage."));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting image for drawing {}", id, e);
            throw new RuntimeException("Failed to get drawing image", e);
        }
    }

    @Transactional
    @CacheEvict(value = "drawings", key = "#id")
    public DrawingResponse update(Long id, Long userId, UpdateDrawingRequest request) {
        log.debug("Updating drawing {} for user {}", id, userId);
        try {
            Drawing drawing = ownedDrawing(id, userId);
            drawing.setDescription(normalizeDescription(request.description()));
            drawings.save(drawing);
            log.info("Updated drawing {} for user {}", id, userId);
            return get(id, userId);
        } catch (NotFoundException | ForbiddenException | BadRequestException e) {
            log.warn("Failed to update drawing {} for user {}: {}", id, userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating drawing {} for user {}", id, userId, e);
            throw new RuntimeException("Failed to update drawing", e);
        }
    }

    @Transactional
    @CacheEvict(value = "drawings", key = "#id")
    public DrawingResponse replaceImage(Long id, Long userId, MultipartFile image) {
        log.debug("Replacing image for drawing {} by user {}", id, userId);
        try {
            Drawing drawing = ownedDrawing(id, userId);
            ValidatedImage validatedImage = validateImage(image);

            // Store new image
            String newStorageKey = storage.store(validatedImage.contentType(), validatedImage.data());

            // Delete old image (best effort)
            if (drawing.getStorageKey() != null) {
                try {
                    storage.delete(drawing.getStorageKey());
                } catch (Exception e) {
                    log.warn("Failed to delete old image for drawing {}: {}", id, e.getMessage());
                }
            }

            drawing.replaceImage(newStorageKey, validatedImage.contentType(), validatedImage.data().length);
            drawings.save(drawing);
            log.info("Replaced image for drawing {} by user {}", id, userId);
            return get(id, userId);
        } catch (NotFoundException | ForbiddenException | BadRequestException e) {
            log.warn("Failed to replace image for drawing {} by user {}: {}", id, userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error replacing image for drawing {} by user {}", id, userId, e);
            throw new RuntimeException("Failed to replace image", e);
        }
    }

    @Transactional
    @CacheEvict(value = "drawings", key = "#id")
    public void delete(Long id, Long userId) {
        log.debug("Deleting drawing {} for user {}", id, userId);
        try {
            Drawing drawing = ownedDrawing(id, userId);

            // Delete image from storage
            if (drawing.getStorageKey() != null) {
                try {
                    storage.delete(drawing.getStorageKey());
                } catch (Exception e) {
                    log.warn("Failed to delete image for drawing {}: {}", id, e.getMessage());
                }
            }

            drawings.delete(drawing);
            log.info("Deleted drawing {} for user {}", id, userId);
        } catch (NotFoundException | ForbiddenException e) {
            log.warn("Failed to delete drawing {} for user {}: {}", id, userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting drawing {} for user {}", id, userId, e);
            throw new RuntimeException("Failed to delete drawing", e);
        }
    }

    private Drawing ownedDrawing(Long id, Long userId) {
        Drawing drawing = drawings.findById(id)
                .orElseThrow(() -> new NotFoundException("Drawing not found."));

        if (!drawing.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only change your own drawings.");
        }

        return drawing;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BadRequestException("Description must be at most 2000 characters.");
        }
        return trimmed;
    }

    private ValidatedImage validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("A drawing image is required.");
        }

        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("The drawing image must be 10 MB or smaller.");
        }

        String contentType = image.getContentType();
        if (contentType == null) {
            throw new BadRequestException("The drawing image type is missing.");
        }

        String normalizedType = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedType)) {
            throw new BadRequestException("Only PNG, JPEG, and WebP images are supported.");
        }

        try {
            return new ValidatedImage(image.getBytes(), normalizedType);
        } catch (IOException exception) {
            throw new BadRequestException("The drawing image could not be read.");
        }
    }

    private String emptyMessage(LocalDate date, LocalDate from, LocalDate to) {
        if (date != null) {
            return "No drawings were found for " + date + ".";
        }
        if (from != null) {
            return "No drawings were found between " + from + " and " + to + ".";
        }
        return "No drawings have been posted yet.";
    }

    private record ValidatedImage(byte[] data, String contentType) {
    }
}
