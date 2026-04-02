/**
 * Lớp Electronics kế thừa Item [cite: 113]
 */
public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String id, String name, String desc, double price, String sellerId, int warranty) {
        super(id, name, desc, price, sellerId);
        this.warrantyMonths = warranty;
    }
    @Override
    public String getInfo() {
        return super.getInfo() + String.format(" | Bảo hành: %i ", warrantyMonths);
    }
}