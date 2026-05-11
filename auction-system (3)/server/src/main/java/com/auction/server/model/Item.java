package com.auction.server.model;

/** Abstract – phân cấp: Electronics / Art / Vehicle */
public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected String sellerId;
    protected String category;

    public Item() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getCategory() { return category; }
}
