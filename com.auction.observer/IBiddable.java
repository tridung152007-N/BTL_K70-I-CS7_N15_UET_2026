package com.auction.observer;

import com.auction.exception.AuctionException;

public interface IBiddable {
    void placeBid(String bidderId, double amount) throws AuctionException;
    double getCurrentPrice();
    boolean isAuctionActive();
}