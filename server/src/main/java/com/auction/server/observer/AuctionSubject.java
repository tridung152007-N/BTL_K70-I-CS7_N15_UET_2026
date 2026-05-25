package com.auction.server.observer;

/** Subject interface cho Observer Pattern. */
public interface AuctionSubject {
    void addObserver(AuctionObserver observer);
    void removeObserver(AuctionObserver observer);
    void notifyBidUpdate(String auctionId, com.auction.server.model.BidTransaction bid);
    void notifyAuctionEnd(String auctionId, String winnerId, double finalPrice);
}
