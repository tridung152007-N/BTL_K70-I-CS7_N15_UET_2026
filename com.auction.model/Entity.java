import java.time.LocalDateTime;

/**
 * Lớp cơ sở Entity (Abstract Class) 
 */
package com.auction.model;

public abstract class Entity {
    private String id; // [cite: 119]
    private LocalDateTime createdAt;

    public Entity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
    }

    // Encapsulation: getter/setter [cite: 119]
    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Abstraction: Phương thức trừu tượng buộc lớp con thực thi [cite: 122]
    public abstract String getInfo();
}