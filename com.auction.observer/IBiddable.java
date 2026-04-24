/**
 * Interface cho các đối tượng có thể đặt giá
 */
package com.auction.observer;

public interface IBiddable {
    void placeBid(String bidderId, double amount) throws AuctionException; // Đặt giá
    double getCurrentPrice();                        // Lấy giá hiện tại
    boolean isAuctionActive();                       // Kiểm tra trạng thái phiên
}