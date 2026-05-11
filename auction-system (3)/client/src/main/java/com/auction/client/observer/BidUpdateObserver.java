package com.auction.client.observer;

import com.auction.server.model.BidTransaction;

public interface BidUpdateObserver {
    void onBidUpdate(BidTransaction bid);
    void onAuctionEnd(String auctionId, String winnerId, double finalPrice);
}
