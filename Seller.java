/**
 * Lớp Seller kế thừa User [cite: 115]
 */
public class Seller extends User {
    public Seller(String id, String username, String password, String email) {
        super(id, username, password, email, "SELLER");
    }

    public void postItem() {
        // Logic quản lý sản phẩm [cite: 38]
        System.out.println("Seller " + getUsername() + " đang đăng sản phẩm mới.");
    }
}