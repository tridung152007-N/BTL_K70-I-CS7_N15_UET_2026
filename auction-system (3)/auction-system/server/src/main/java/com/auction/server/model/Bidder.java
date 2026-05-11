package com.auction.server.model;

public class Bidder extends User {
    private double balance;

    public Bidder() { this.role = "BIDDER"; }
    public Bidder(String id, String username, String passwordHash, String email) {
        super(id, username, passwordHash, email, "BIDDER");
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    @Override
    public void printInfo() {
        System.out.println("[Bidder] " + username + " | balance=" + balance);
    }
}
