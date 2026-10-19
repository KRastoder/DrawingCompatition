package com.drawing_compatition.KRastoder.drawing;

import com.drawing_compatition.KRastoder.common.BadRequestException;
import com.drawing_compatition.KRastoder.common.ForbiddenException;
import com.drawing_compatition.KRastoder.common.NotFoundException;
import com.drawing_compatition.KRastoder.user.User;
import com.drawing_compatition.KRastoder.user.UserRepository;
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

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private final DrawingRepository drawings;
    private final UserRepository users;

    public DrawingService(DrawingRepository drawings, UserRepository users) {
        this.drawings = drawings;
        this.users = users;
    }

    @Transactional
    public DrawingResponse create(Long userId, String rawDescription, MultipartFile image) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));

        String description = normalizeDescription(rawDescription);
        ValidatedImage validatedImage = validateImage(image);

        Drawing drawing = drawings.save(new Drawing(
                user,
                description,
                validatedImage.data(),
                validatedImage.contentType()
        ));

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
    }

    @Transactional(readOnly = true)
    public List<DrawingResponse> list(Long viewerId, LocalDate date, LocalDate from, LocalDate to) {
        return listPage(viewerId, date, from, to, 0, Integer.MAX_VALUE).getContent();
    }

    @Transactional(readOnly = true)
    public Page<DrawingResponse> listPage(Long viewerId, LocalDate date, LocalDate from, LocalDate to, int page, int size) {
        DateRange range = DateRange.of(date, from, to);
        List<DrawingSummary> allSummaries = drawings.findSummaries(range.from(), range.to(), viewerId);

        boolean hasDateFilter = date != null || from != null;

        if (allSummaries.isEmpty() && hasDateFilter && page == 0) {
            throw new NotFoundException(emptyMessage(date, from, to));
        }

        // Apply sorting manually since we can't use Pageable with custom @Query
        allSummaries.sort((a, b) -> {
            int voteCompare = Long.compare(b.voteCount(), a.voteCount());
            if (voteCompare != 0) return voteCompare;
            int dateCompare = b.createdAt().compareTo(a.createdAt());
            if (dateCompare != 0) return dateCompare;
            return b.id().compareTo(a.id());
        });

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, allSummaries.size());
        List<DrawingSummary> pageContent = start >= allSummaries.size() ? List.of() : allSummaries.subList(start, end);

        return new PageImpl<>(pageContent.stream().map(DrawingResponse::from).toList(), PageRequest.of(page, size), allSummaries.size());
    }

    @Transactional(readOnly = true)
    public List<DrawingResponse> listMine(Long userId) {
        return listMinePage(userId, 0, Integer.MAX_VALUE).getContent();
    }

    @Transactional(readOnly = true)
    public Page<DrawingResponse> listMinePage(Long userId, int page, int size) {
        List<DrawingSummary> allSummaries = drawings.findSummariesByOwner(userId, userId);

        // Sort by createdAt desc, id desc
        allSummaries.sort((a, b) -> {
            int dateCompare = b.createdAt().compareTo(a.createdAt());
            if (dateCompare != 0) return dateCompare;
            return b.id().compareTo(a.id());
        });

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, allSummaries.size());
        List<DrawingSummary> pageContent = start >= allSummaries.size() ? List.of() : allSummaries.subList(start, end);

        return new PageImpl<>(pageContent.stream().map(DrawingResponse::from).toList(), PageRequest.of(page, size), allSummaries.size());
    }

    @Transactional(readOnly = true)
    public DrawingResponse get(Long id, Long viewerId) {
        return drawings.findSummaryById(id, viewerId)
                .map(DrawingResponse::from)
                .orElseThrow(() -> new NotFoundException("Drawing not found."));
    }

    @Transactional(readOnly = true)
    public DrawingImage getImage(Long id) {
        return drawings.findImageById(id)
                .orElseThrow(() -> new NotFoundException("Drawing not found."));
    }

    @Transactional
    public DrawingResponse update(Long id, Long userId, UpdateDrawingRequest request) {
        Drawing drawing = ownedDrawing(id, userId);
        drawing.setDescription(normalizeDescription(request.description()));
        drawings.save(drawing);
        return get(id, userId);
    }

    @Transactional
    public DrawingResponse replaceImage(Long id, Long userId, MultipartFile image) {
        Drawing drawing = ownedDrawing(id, userId);
        ValidatedImage validatedImage = validateImage(image);
        drawing.replaceImage(validatedImage.data(), validatedImage.contentType());
        drawings.save(drawing);
        return get(id, userId);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Drawing drawing = ownedDrawing(id, userId);
        drawings.delete(drawing);
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
