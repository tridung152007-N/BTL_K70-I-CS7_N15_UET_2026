public class ItemFactory {
    public static Items createItem(String type, String id, String name, double price, String sellerId) {
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                return new Electronics(id, name, "Mô tả...", price, sellerId, 12);
            case "ARTS":
                return new Arts(id, name, "Mô tả...", price, sellerId, "Tác giả", "Chất liệu");
            case "VEHICLE":
                return new Vehicle(id, name, "Mô tả...", price, sellerId, "Số khung", 2024);
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ!");
        }
    }
}