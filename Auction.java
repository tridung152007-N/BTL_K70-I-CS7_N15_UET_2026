import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity implements IBiddable, ISearchable {
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private String highestBidderId;
    
    public enum AuctionStatus { OPEN, RUNNING, FINISHED, PAID, CANCELED }
    private AuctionStatus status; 

    // Danh sách các người quan sát (Observers) [cite: 72]
    private final List<AuctionObserver> observers = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Auction(String id, Item item, LocalDateTime start, LocalDateTime end) {
        super(id);
        this.item = item;
        this.startTime = start;
        this.endTime = end;
        this.currentPrice = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
    }

    // Đăng ký người quan sát mới (ví dụ: một ClientHandler mới kết nối) [cite: 129]
    public void addObserver(AuctionObserver observer) {
        lock.lock();
        try {
            observers.add(observer);
        } finally {
            lock.unlock();
        }
    }

    public void removeObserver(AuctionObserver observer) {
        lock.lock();
        try {
            observers.remove(observer);
        } finally {
            lock.unlock();
        }
    }

    // Thông báo cho tất cả người quan sát khi giá thay đổi 
    private void notifyObservers() {
        for (AuctionObserver observer : observers) {
            observer.onPriceUpdate(this.getId(), this.currentPrice, this.highestBidderId);
        }
    }

    @Override
    public boolean placeBid(String bidderId, double amount) {
        lock.lock(); 
        try {
            // Kiểm tra trạng thái và thời gian [cite: 20, 73, 88]
            if (status != AuctionStatus.RUNNING || LocalDateTime.now().isAfter(endTime)) {
                return false; 
            }

            // Kiểm tra giá đặt hợp lệ [cite: 20, 88]
            if (amount <= currentPrice) {
                return false;
            }

            this.currentPrice = amount;
            this.highestBidderId = bidderId;
            
            // Kích hoạt thông báo Realtime ngay sau khi cập nhật thành công 
            notifyObservers();
            
            return true;
        } finally {
            lock.unlock(); 
        }
    }

    public void updateStatus() {
        lock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            // Logic chuyển trạng thái: OPEN -> RUNNING -> FINISHED [cite: 74, 75, 76]
            if (status == AuctionStatus.OPEN && now.isAfter(startTime) && now.isBefore(endTime)) {
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

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public double getCurrentPrice() { return currentPrice; }
}