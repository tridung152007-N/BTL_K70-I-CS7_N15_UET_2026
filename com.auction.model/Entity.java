package com.auction.model;

import java.time.LocalDateTime;

/**
 * Lớp cơ sở Entity (Abstract Class)
 */
public abstract class Entity {
    private String id;
    private LocalDateTime createdAt;

    public Entity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public abstract String getInfo();
}