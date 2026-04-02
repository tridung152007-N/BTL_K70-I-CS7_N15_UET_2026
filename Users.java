/**
 * Lớp User kế thừa Entity 
 */
public abstract class Users extends Entity {
    private String username;
    private String password;
    private String email;
    private String role; // [cite: 34]

    public Users(String id, String username, String password, String email, String role) {
        super(id);
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }

    // Polymorphism: Ghi đè phương thức từ Entity [cite: 121]
    @Override
    public String getInfo() {
        return String.format("User [%s]: %s (Role: %s)", getId(), username, role);
    }
}