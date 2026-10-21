package com.drawing_compatition.KRastoder.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "avatar_storage_key", length = 200)
    private String avatarStorageKey;

    @Column(name = "avatar_content_type", length = 100)
    private String avatarContentType;

    @Column(name = "avatar_size")
    private Long avatarSize;

    protected User() {
    }

    public User(String username, String displayName, String passwordHash) {
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarStorageKey() {
        return avatarStorageKey;
    }

    public String getAvatarContentType() {
        return avatarContentType;
    }

    public Long getAvatarSize() {
        return avatarSize;
    }

    public boolean isHasAvatar() {
        return avatarStorageKey != null && !avatarStorageKey.isBlank();
    }

    public void setAvatar(String avatarStorageKey, String avatarContentType, long avatarSize) {
        this.avatarStorageKey = avatarStorageKey;
        this.avatarContentType = avatarContentType;
        this.avatarSize = avatarSize;
    }

    public void clearAvatar() {
        this.avatarStorageKey = null;
        this.avatarContentType = null;
        this.avatarSize = null;
    }
}
