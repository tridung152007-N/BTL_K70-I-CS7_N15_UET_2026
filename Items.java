/**
 * Lớp Item kế thừa Entity [cite: 112]
 */
public abstract class Items extends Entity {
    private String itemName;
    private String description;
    private double startingPrice; // [cite: 42]
    private String sellerId;

    public Items(String id, String itemName, String description, double startingPrice, String sellerId) {
        super(id);
        this.itemName = itemName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }

    public String getItemName() { return itemName; }
    public double getStartingPrice() { return startingPrice; }

    @Override
    public String getInfo() {
        return String.format("Item [%s]: %s - Giá khởi điểm: %.2f", getId(), itemName, startingPrice);
    }
}