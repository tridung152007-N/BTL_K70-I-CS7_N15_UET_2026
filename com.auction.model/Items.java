package com.auction.model;

/**
 * Lớp Item kế thừa Entity (Abstract)
 */
public abstract class Items extends Entity {
    private String name;
    private String desc;
    private double startingPrice;
    private String sellerId;

    public Items(String id, String name, String desc, double startingPrice, String sellerId) {
        super(id);
        this.name = name;
        this.desc = desc;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }

    public String getItemName() { return name; }
    public String getDesc() { return desc; }
    public double getStartingPrice() { return startingPrice; }
    public String getSellerId() { return sellerId; }

    @Override
    public String getInfo() {
        return String.format("Item [%s]: %s - Giá khởi điểm: %.2f", getId(), name, startingPrice);
    }
}