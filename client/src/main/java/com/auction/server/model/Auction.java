package com.auction.server.model;

import com.auction.common.model.AuctionState;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Quản lý toàn bộ vòng đời một phiên đấu giá. */
public class    Auction extends Entity {
    private String itemId;
    private String sellerId;
    private double startingPrice;
    private double currentPrice;
    private String currentLeaderId;
    private String currentLeaderName;
    private boolean paid;
    private AuctionState state;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<BidTransaction> bidHistory;
    private String itemName;
    private String itemImagePath;

    public Auction() {
        this.state = AuctionState.OPEN;
        this.bidHistory = new ArrayList<>();
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public String getCurrentLeaderId() { return currentLeaderId; }
    public void setCurrentLeaderId(String currentLeaderId) { this.currentLeaderId = currentLeaderId; }
    public String getCurrentLeaderName() { return currentLeaderName; }
    public void setCurrentLeaderName(String currentLeaderName) { this.currentLeaderName = currentLeaderName; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public AuctionState getState() { return state; }
    public void setState(AuctionState state) { this.state = state; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public void addBid(BidTransaction bid) { this.bidHistory.add(bid); }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getItemImagePath() { return itemImagePath; }
    public void setItemImagePath(String itemImagePath) { this.itemImagePath = itemImagePath; }

    @Override
    public void printInfo() {
        System.out.println("[Auction] id=" + id + " | item=" + itemId
                + " | price=" + currentPrice + " | state=" + state);
    }
}
