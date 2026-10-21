package com.drawing_compatition.KRastoder.common;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredBlobRepository extends JpaRepository<StoredBlob, Long> {

    Optional<StoredBlob> findByStorageKey(String storageKey);
}