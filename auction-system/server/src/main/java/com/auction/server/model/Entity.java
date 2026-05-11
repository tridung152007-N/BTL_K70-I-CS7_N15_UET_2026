package com.auction.server.model;

/** Lớp gốc cho tất cả entity trong hệ thống. */
public abstract class Entity {
    protected String id;
    protected long createdAt;

    public Entity() { this.createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getCreatedAt() { return createdAt; }

    public abstract void printInfo();
}
