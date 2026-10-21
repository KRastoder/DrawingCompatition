package com.drawing_compatition.KRastoder.common;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * Abstraction for storing and retrieving binary objects (images, avatars).
 * Allows swapping between local database storage (dev) and object storage (prod).
 */
public interface StorageService {

    /**
     * Store a new object and return its storage key.
     * @param contentType MIME type (e.g., "image/png")
     * @param data binary content
     * @return unique storage key (used for retrieval)
     */
    String store(String contentType, byte[] data);

    /**
     * Store a multipart file and return its storage key.
     */
    default String store(MultipartFile file) throws IOException {
        return store(file.getContentType(), file.getBytes());
    }

    /**
     * Retrieve object data by storage key.
     * @return Optional containing data and content type, or empty if not found
     */
    Optional<StoredObject> retrieve(String storageKey);

    /**
     * Delete an object by storage key.
     * @return true if deleted, false if not found
     */
    boolean delete(String storageKey);

    /**
     * Check if an object exists.
     */
    boolean exists(String storageKey);

    /**
     * Generate a publicly accessible URL for the object (if supported).
     * For local storage, this returns a proxy endpoint like /api/storage/{key}.
     * For S3, this could return a presigned URL or CDN URL.
     */
    String getPublicUrl(String storageKey);

    /**
     * Record representing a stored object.
     */
    record StoredObject(byte[] data, String contentType, long size) {
    }
}