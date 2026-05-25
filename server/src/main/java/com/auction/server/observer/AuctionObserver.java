package com.auction.server.observer;

import com.auction.server.model.BidTransaction;

/** Observer interface – nhận thông báo mỗi khi có bid mới. */
public interface AuctionObserver {
    void onBidUpdate(String auctionId, BidTransaction bid);
    void onAuctionEnd(String auctionId, String winnerId, double finalPrice);
}
