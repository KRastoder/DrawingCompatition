package com.drawing_compatition.KRastoder.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * S3-compatible object storage implementation (AWS S3, Cloudflare R2, MinIO, etc.).
 * Enabled when {@code app.storage.type=s3} and AWS credentials are configured.
 * 
 * TODO: Implement with AWS SDK v2 or Spring Cloud AWS when needed for production.
 * For now, this is a skeleton showing the intended interface.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final String bucketName;
    private final String region;
    private final String endpoint; // For R2/MinIO custom endpoints
    private final String cdnBaseUrl; // Optional CDN base URL for public URLs

    public S3StorageService(
            @Value("${app.storage.s3.bucket}") String bucketName,
            @Value("${app.storage.s3.region}") String region,
            @Value("${app.storage.s3.endpoint:}") String endpoint,
            @Value("${app.storage.s3.cdn-base-url:}") String cdnBaseUrl
    ) {
        this.bucketName = bucketName;
        this.region = region;
        this.endpoint = endpoint;
        this.cdnBaseUrl = cdnBaseUrl;
    }

    @Override
    public String store(String contentType, byte[] data) {
        String key = generateKey(contentType);
        // TODO: Put object to S3
        // s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).contentType(contentType).build(),
        //         RequestBody.fromBytes(data));
        return key;
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        return store(file.getContentType(), file.getBytes());
    }

    @Override
    public Optional<StoredObject> retrieve(String storageKey) {
        // TODO: Get object from S3
        // GetObjectResponse response = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(storageKey).build());
        // return Optional.of(new StoredObject(response.readAllBytes(), response.contentType(), response.contentLength()));
        return Optional.empty();
    }

    @Override
    public boolean delete(String storageKey) {
        // TODO: Delete object from S3
        // s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(storageKey).build());
        return true;
    }

    @Override
    public boolean exists(String storageKey) {
        // TODO: Head object
        return false;
    }

    @Override
    public String getPublicUrl(String storageKey) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isBlank()) {
            return cdnBaseUrl + "/" + storageKey;
        }
        // Generate presigned URL (valid for 1 hour)
        // URL url = s3Client.utilities().getUrl(GetUrlRequest.builder().bucket(bucketName).key(storageKey).build());
        // return url.toString();
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + storageKey;
    }

    private String generateKey(String contentType) {
        String ext = switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> "bin";
        };
        return "drawings/" + UUID.randomUUID() + "." + ext;
    }
}