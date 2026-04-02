/**
 * Lớp Bidder kế thừa User [cite: 115]
 */
public class Bidder extends User {
    private double balance;

    public Bidder(String id, String username, String password, String email, double balance) {
        super(id, username, password, email, "BIDDER");
        this.balance = balance;
    }

    public void placeBid(double amount) {
        // Logic tham gia đấu giá [cite: 47]
        System.out.println("Bidder " + getUsername() + " đặt giá: " + amount);
    }
}