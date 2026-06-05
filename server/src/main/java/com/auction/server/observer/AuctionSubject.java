package com.auction.server.observer;

import com.auction.server.model.BidTransaction;

public interface AuctionSubject {
    void addObserver(com.auction.server.observer.AuctionObserver o);
    void removeObserver(com.auction.server.observer.AuctionObserver o);
    void notifyBidUpdate(String auctionId, BidTransaction bid);
    void notifyAuctionEnd(String auctionId, String winnerId, double finalPrice);
}