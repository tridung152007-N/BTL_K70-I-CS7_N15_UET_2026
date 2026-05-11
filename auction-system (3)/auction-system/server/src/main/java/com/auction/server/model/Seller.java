package com.auction.server.model;

public class Seller extends User {
    private String shopName;

    public Seller() { this.role = "SELLER"; }
    public Seller(String id, String username, String passwordHash, String email, String shopName) {
        super(id, username, passwordHash, email, "SELLER");
        this.shopName = shopName;
    }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    @Override
    public void printInfo() {
        System.out.println("[Seller] " + username + " | shop=" + shopName);
    }
}
