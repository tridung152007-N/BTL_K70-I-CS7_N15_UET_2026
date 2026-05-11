package com.auction.server.model;

/** Ghi nhận mỗi lần đặt giá hợp lệ. */
public class BidTransaction extends Entity {
    private String auctionId;
    private String bidderId;
    private double amount;
    private boolean isAutoBid;

    public BidTransaction() {}

    public BidTransaction(String auctionId, String bidderId, double amount, boolean isAutoBid) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.isAutoBid = isAutoBid;
        this.createdAt = System.currentTimeMillis();
    }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public boolean isAutoBid() { return isAutoBid; }
    public void setAutoBid(boolean autoBid) { isAutoBid = autoBid; }

    @Override
    public void printInfo() {
        System.out.println("[BidTransaction] auction=" + auctionId
                + " | bidder=" + bidderId + " | amount=" + amount);
    }
}
