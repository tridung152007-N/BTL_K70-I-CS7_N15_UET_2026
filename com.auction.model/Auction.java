package com.auction.model;

import com.auction.observer.AuctionObserver;
import java.time.LocalDateTime;
import com.auction.exception.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private Items item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private String highestBidderId;
    
    public enum AuctionStatus { OPEN, RUNNING, FINISHED, PAID, CANCELED }
    private AuctionStatus status; 

    private final List<AuctionObserver> observers = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Auction(String id, Item item, LocalDateTime start, LocalDateTime end) {
        super(id);
        this.item = item;
        this.startTime = start;
        this.endTime = end;
        this.currentPrice = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
    }

    // Cập nhật trạng thái dựa trên thời gian thực
    public void updateStatus() {
        lock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            if (status == AuctionStatus.OPEN && now.isAfter(startTime) && now.isBefore(endTime)) {
                this.status = AuctionStatus.RUNNING;
            } else if (status == AuctionStatus.RUNNING && now.isAfter(endTime)) {
                this.status = AuctionStatus.FINISHED;
            }
        } finally {
            lock.unlock();
        }
    }

    // Cập nhật hàm đặt giá để ném ngoại lệ thay vì trả về boolean
    public void placeBid(String bidderId, double amount) throws AuctionException {
        lock.lock(); 
        try {
            updateStatus(); 

            if (status != AuctionStatus.RUNNING) {
                throw new AuctionException("Phiên đấu giá hiện không trong trạng thái cho phép đặt giá.");
            }
            if (amount <= currentPrice) {
                throw new InvalidBidException(amount, currentPrice);
            }

            this.currentPrice = amount;
            this.highestBidderId = bidderId;
            
            notifyObservers(); 
        } finally {
            lock.unlock(); 
        }
    }

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        for (AuctionObserver observer : observers) {
            observer.onPriceUpdate(this.getId(), this.currentPrice, this.highestBidderId);
        }
    }

    // Getters
    public double getCurrentPrice() { return currentPrice; }
    public String getHighestBidderId() { return highestBidderId; }
    public AuctionStatus getStatus() { return status; }
    public String getInfo() {
        return String.format("Auction [%s]: %s | Giá: %.2f | Trạng thái: %s", 
                getId(), item.getItemName(), currentPrice, status);
    }
}