package com.auction.observer;

public interface AuctionObserver {
    void onPriceUpdate(String auctionId, double newPrice, String highestBidderId);
}