import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity implements IBiddable, ISearchable {
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private String highestBidderId;
    private String status; // OPEN, RUNNING, FINISHED 

    public Auction(String id, Item item, LocalDateTime start, LocalDateTime end) {
        super(id);
        this.item = item;
        this.startTime = start;
        this.endTime = end;
        this.currentPrice = item.getStartingPrice();
        this.status = "OPEN";
    }

    @Override
    public boolean placeBid(String bidderId, double amount) {
        if (amount > currentPrice && LocalDateTime.now().isBefore(endTime)) {
            this.currentPrice = amount;
            this.highestBidderId = bidderId;
            return true;
        }
        return false; [cite: 48, 49]
    }

    @Override
    public boolean match(String keyword) {
        return item.getItemName().toLowerCase().contains(keyword.toLowerCase());
    }

    @Override
    public String getInfo() {
        return String.format("Auction [%s]: %s | Giá hiện tại: %.2f | Kết thúc: %s", 
                getId(), item.getItemName(), currentPrice, endTime);
    }

    // Getters cho GUI và Server xử lý [cite: 119]
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}