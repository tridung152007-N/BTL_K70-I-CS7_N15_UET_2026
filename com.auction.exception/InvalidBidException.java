package com.auction.exception;

public class InvalidBidException extends AuctionException {
    public InvalidBidException(double bid, double current) {
        super(String.format("Giá đặt %.2f phải cao hơn giá hiện tại %.2f", bid, current));
    }
}