package com.auction.client.session;

public class UserSession {
    private static UserSession instance;

    private String userId;
    private String username;
    private String role;
    private double balance;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public double getBalance() { return balance; }

    public void login(String userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public void login(String userId, String username, String role, double balance) {
        login(userId, username, role);
        this.balance = balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void logout() {
        this.userId = null;
        this.username = null;
        this.role = null;
        this.balance = 0;
    }

    public boolean isLoggedIn() {
        return userId != null;
    }
}
