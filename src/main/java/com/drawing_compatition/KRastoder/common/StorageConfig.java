package com.drawing_compatition.KRastoder.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Storage configuration - selects implementation based on app.storage.type property.
 * - local (default): LocalStorageService using stored_blobs table
 * - s3: S3StorageService using AWS S3 / R2 / MinIO
 */
@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "s3", matchIfMissing = false)
    public StorageService s3StorageService(
            @org.springframework.beans.factory.annotation.Value("${app.storage.s3.bucket}") String bucketName,
            @org.springframework.beans.factory.annotation.Value("${app.storage.s3.region}") String region,
            @org.springframework.beans.factory.annotation.Value("${app.storage.s3.endpoint:}") String endpoint,
            @org.springframework.beans.factory.annotation.Value("${app.storage.s3.cdn-base-url:}") String cdnBaseUrl
    ) {
        return new S3StorageService(bucketName, region, endpoint, cdnBaseUrl);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
    public StorageService localStorageService(StoredBlobRepository blobRepository) {
        return new LocalStorageService(blobRepository);
    }
}