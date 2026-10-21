package com.drawing_compatition.KRastoder.drawing;

import com.drawing_compatition.KRastoder.user.User;
import com.drawing_compatition.KRastoder.vote.Vote;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drawings")
public class Drawing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "storage_key", nullable = false, length = 200)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "image_size", nullable = false)
    private long imageSize;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "drawing", fetch = FetchType.LAZY)
    private List<Vote> votes = new ArrayList<>();

    protected Drawing() {
    }

    public Drawing(User user, String description, String storageKey, String contentType, long imageSize) {
        this.user = user;
        this.description = description;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.imageSize = imageSize;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getDescription() {
        return description;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getImageSize() {
        return imageSize;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void replaceImage(String storageKey, String contentType, long imageSize) {
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.imageSize = imageSize;
        this.updatedAt = Instant.now();
    }
}
