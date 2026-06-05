package com.auction.server.observer;

import com.auction.server.model.BidTransaction;

public interface AuctionObserver {
    void onBidUpdate(String auctionId, BidTransaction bid);
    void onAuctionEnd(String auctionId, String winnerId, double finalPrice);
}