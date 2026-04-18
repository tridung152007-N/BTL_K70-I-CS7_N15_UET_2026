/**
 * Lớp Seller kế thừa User [cite: 115]
 */
package com.auction.model;

public class Seller extends Users {
    public Seller(String id, String username, String password, String email) {
        super(id, username, password, email, "SELLER");
    }

    public void postItem() {
        // Logic quản lý sản phẩm [cite: 38]
        System.out.println("Seller " + getUsername() + " đang đăng sản phẩm mới.");
    }
}