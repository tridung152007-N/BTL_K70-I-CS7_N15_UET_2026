/**
 * Lớp Vehicle kế thừa Item
 */
public class Vehicles extends Items {
    private String engineNumber; // Số máy/Số khung
    private int manufacturingYear; // Năm sản xuất

    public Vehicles(String id, String name, String desc, double price, String sellerId, String engineNumber, int year) {
        super(id, name, desc, price, sellerId);
        this.engineNumber = engineNumber;
        this.manufacturingYear = year;
    }

    @Override
    public String getInfo() {
        return super.getInfo() + String.format(" | Số máy: %s | Năm SX: %d", engineNumber, manufacturingYear);
    }
}