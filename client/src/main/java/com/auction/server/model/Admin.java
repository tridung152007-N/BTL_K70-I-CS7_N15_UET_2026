package com.auction.server.model;

public class Admin extends User {
    public Admin() { this.role = "ADMIN"; }
    public Admin(String id, String username, String passwordHash, String email) {
        super(id, username, passwordHash, email, "ADMIN");
    }

    @Override
    public void printInfo() {
        System.out.println("[Admin] " + username);
    }
}
