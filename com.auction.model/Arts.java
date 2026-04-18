/**
 * Lớp Arts kế thừa Item
 */
package com.auction.model;

public class Arts extends Items {
    private String artistName; // Tên họa sĩ/tác giả
    private String material;   // Chất liệu (Sơn dầu, gỗ, đá...)

    public Arts(String id, String name, String desc, double price, String sellerId, String artistName, String material) {
        super(id, name, desc, price, sellerId);
        this.artistName = artistName;
        this.material = material;
    }

    // Ghi đè để hiển thị thông tin đặc thù của đồ nghệ thuật
    @Override
    public String getInfo() {
        return super.getInfo() + String.format(" | Tác giả: %s | Chất liệu: %s", artistName, material);
    }
}