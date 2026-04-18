package com.auction.controller;

import com.auction.model.Auction;
import com.auction.factory.AuctionManager;
import com.auction.exception.AuctionException; // Thêm import

public class AuctionController {
    private final AuctionManager auctionManager = AuctionManager.getInstance();

    public String handlePlaceBid(String auctionId, String bidderId, double amount) {
        try {
            Auction auction = auctionManager.findAuctionById(auctionId);
            
            if (auction == null) {
                throw new AuctionException("Không tìm thấy phiên đấu giá.");
            }

            // Gọi logic ném ngoại lệ từ Model
            auction.placeBid(bidderId, amount);

            return "SUCCESS: Đặt giá thành công " + amount;

        } catch (AuctionException e) {
            // Trả về thông báo lỗi cụ thể từ ngoại lệ
            return "ERROR: " + e.getMessage();
        }
    }

    public String getAuctionDetails(String auctionId) {
        Auction auction = auctionManager.findAuctionById(auctionId);
        return (auction != null) ? auction.getInfo() : "ERROR: Not found";
    }
}