/**
 * Interface cho các đối tượng có thể đặt giá
 */
public interface IBiddable {
    boolean placeBid(String bidderId, double amount); // Đặt giá
    double getCurrentPrice();                        // Lấy giá hiện tại
    boolean isAuctionActive();                       // Kiểm tra trạng thái phiên
}