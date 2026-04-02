import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private String auctionId;
    private String bidderId;
    private double bidAmount;
    private LocalDateTime timestamp;

    public BidTransaction(String id, String auctionId, String bidderId, double bidAmount) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String getInfo() {
        return String.format("[%s] Bidder %s đặt %.2f cho phiên %s", 
                timestamp, bidderId, bidAmount, auctionId);
    }

    // Getters để lấy dữ liệu vẽ biểu đồ [cite: 102]
    public double getBidAmount() { return bidAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}