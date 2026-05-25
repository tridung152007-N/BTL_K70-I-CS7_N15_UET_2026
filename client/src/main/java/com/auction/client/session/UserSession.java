package com.auction.client.session;

/** Singleton lưu thông tin user sau khi đăng nhập thành công. */
public class UserSession {
    private static UserSession instance;

    private String userId;
    private String username;
    private String role;   // "SELLER" hoặc "BIDDER"

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public String getUserId()   { return userId; }
    public String getUsername() { return username; }
    public String getRole()     { return role; }

    public void login(String userId, String username, String role) {
        this.userId   = userId;
        this.username = username;
        this.role     = role;
    }

    public void logout() {
        this.userId   = null;
        this.username = null;
        this.role     = null;
    }

    public boolean isLoggedIn() { return userId != null; }
}
