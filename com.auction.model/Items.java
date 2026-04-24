/**
 * Lớp Item kế thừa Entity [cite: 112]
 */
package com.auction.model;

public abstract class Items extends Entity {
    private String name;
    private String desc;
    private double price; // [cite: 42]
    private String sellerId;

    public Items(String id, String name, String desc, double price, String sellerId) {
        super(id);
        this.name = name;
        this.desc = desc;
        this.price = price;
        this.sellerId = sellerId;
    }

    public String getItemName() { return name; }
    public double getStartingPrice() { return price; }

    @Override
    public String getInfo() {
        return String.format("Item [%s]: %s - Giá khởi điểm: %.2f", getId(), name, price);
    }
  
    @Override
    public boolean isAuctionActive() {
        return this.status == AuctionStatus.RUNNING;
    }


    public String getId() {
        return super.getId(); 
    }
}