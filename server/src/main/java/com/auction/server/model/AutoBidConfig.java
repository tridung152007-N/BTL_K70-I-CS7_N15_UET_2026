package com.auction.server.model;

/** Cấu hình Auto-Bidding của một bidder trong một phiên. */
public class AutoBidConfig {
    private String bidderId;
    private String auctionId;
    private double maxBid;
    private double increment;
    private long registeredAt;

    public AutoBidConfig(String bidderId, String auctionId, double maxBid, double increment) {
        this.bidderId = bidderId;
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = System.currentTimeMillis();
    }

    public String getBidderId() { return bidderId; }
    public String getAuctionId() { return auctionId; }
    public double getMaxBid() { return maxBid; }
    public double getIncrement() { return increment; }
    public long getRegisteredAt() { return registeredAt; }
}
