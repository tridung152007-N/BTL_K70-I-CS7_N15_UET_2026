package com.auction.model;

/**
 * Lớp Electronics kế thừa Items
 */
public class Electronics extends Items {
    private int warrantyMonths;

    public Electronics(String id, String name, String desc, double price, String sellerId, int warrantyMonths) {
        super(id, name, desc, price, sellerId);
        this.warrantyMonths = warrantyMonths;
    }

    public int getWarrantyMonths() { return warrantyMonths; }

    @Override
    public String getInfo() {
        return super.getInfo() + String.format(" | Bảo hành: %d tháng", warrantyMonths);
    }
}