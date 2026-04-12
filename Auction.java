import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock; // Import để xử lý concurrency [cite: 226]

public class Auction extends Entity implements IBiddable, ISearchable {
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private String highestBidderId;
    
    // Trạng thái phiên theo tài liệu: OPEN -> RUNNING -> FINISHED -> PAID / CANCELED 
    public enum AuctionStatus { OPEN, RUNNING, FINISHED, PAID, CANCELED }
    private AuctionStatus status; 

    // Sử dụng ReentrantLock để tránh Lost Update và Race Condition 
    private final ReentrantLock lock = new ReentrantLock();

    public Auction(String id, Item item, LocalDateTime start, LocalDateTime end) {
        super(id);
        this.item = item;
        this.startTime = start;
        this.endTime = end;
        this.currentPrice = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
    }

    @Override
    public boolean placeBid(String bidderId, double amount) {
        lock.lock(); // Bắt đầu khóa để đảm bảo chỉ 1 thread được xử lý đặt giá tại 1 thời điểm [cite: 226]
        try {
            // Kiểm tra tính hợp lệ: Phiên phải đang RUNNING và chưa hết giờ [cite: 49, 59, 221, 236]
            if (status != AuctionStatus.RUNNING || LocalDateTime.now().isAfter(endTime)) {
                return false; 
            }

            // Kiểm tra giá đặt phải cao hơn giá hiện tại [cite: 48, 58, 236]
            if (amount <= currentPrice) {
                return false;
            }

            // Cập nhật thông tin người dẫn đầu [cite: 50]
            this.currentPrice = amount;
            this.highestBidderId = bidderId;
            
            // Ở tuần 7, bạn sẽ gọi notifyObservers() tại đây để cập nhật realtime [cite: 94, 220]
            return true;
        } finally {
            lock.unlock(); // Luôn giải phóng khóa [cite: 226]
        }
    }

    // Logic chuyển trạng thái phiên đấu giá [cite: 222, 224]
    public void updateStatus() {
        lock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            if (status == AuctionStatus.OPEN && now.isAfter(startTime)) {
                this.status = AuctionStatus.RUNNING;
            } else if (status == AuctionStatus.RUNNING && now.isAfter(endTime)) {
                this.status = AuctionStatus.FINISHED;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean match(String keyword) {
        return item.getItemName().toLowerCase().contains(keyword.toLowerCase());
    }

    @Override
    public String getInfo() {
        return String.format("Auction [%s]: %s | Giá hiện tại: %.2f | Trạng thái: %s | Kết thúc: %s", 
                getId(), item.getItemName(), currentPrice, status, endTime);
    }

    // Encapsulation: Getter/Setter [cite: 119]
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public double getCurrentPrice() { return currentPrice; }
}