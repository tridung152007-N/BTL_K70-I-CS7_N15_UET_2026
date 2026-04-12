public interface AuctionObserver {
    // Được gọi để gửi dữ liệu giá mới về máy khách
    void onPriceUpdate(String auctionId, double newPrice, String highestBidderId);
}