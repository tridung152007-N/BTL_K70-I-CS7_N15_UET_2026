package com.auction.server.model;

/** Abstract – phân cấp: Bidder / Seller / Admin */
public abstract class User extends Entity {
    protected String username;
    protected String passwordHash;
    protected String email;
    protected String role;

    public User() {}
    public User(String id, String username, String passwordHash, String email, String role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
}
