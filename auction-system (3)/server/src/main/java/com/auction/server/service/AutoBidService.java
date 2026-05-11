package com.auction.server.service;

import com.auction.server.model.AutoBidConfig;
import com.auction.server.model.BidTransaction;
import com.auction.server.observer.AuctionObserver;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityQueue;

/**
 * Xử lý Auto-Bidding – ưu tiên theo thời gian đăng ký (FIFO).
 */
public class AutoBidService implements AuctionObserver {

    private final AuctionService auctionService;
    // auctionId → danh sách auto-bid (sắp xếp theo registeredAt)
    private final Map<String, PriorityQueue<AutoBidConfig>> autoBidMap = new HashMap<>();
    // FIX Bug 1: dùng thread riêng để tránh deadlock với ReentrantLock của AuctionService
    private final ExecutorService autoBidExecutor = Executors.newSingleThreadExecutor();

    public AutoBidService(AuctionService auctionService) {
        this.auctionService = auctionService;
        auctionService.addObserver(this);
    }

    public void registerAutoBid(AutoBidConfig config) {
        autoBidMap.computeIfAbsent(config.getAuctionId(),
                k -> new PriorityQueue<>(Comparator.comparingLong(AutoBidConfig::getRegisteredAt)))
                .add(config);
    }

    public void cancelAutoBid(String auctionId, String bidderId) {
        PriorityQueue<AutoBidConfig> queue = autoBidMap.get(auctionId);
        if (queue != null) queue.removeIf(c -> c.getBidderId().equals(bidderId));
    }

    @Override
    public void onBidUpdate(String auctionId, BidTransaction lastBid) {
        PriorityQueue<AutoBidConfig> queue = autoBidMap.get(auctionId);
        if (queue == null || queue.isEmpty()) return;

        // FIX Bug 2: dùng peek() để lấy phần tử ưu tiên cao nhất (registeredAt nhỏ nhất)
        // thay vì for-each không đảm bảo thứ tự trên PriorityQueue
        // FIX Bug 1: dispatch sang thread riêng để tránh deadlock (placeBid đang giữ lock)
        autoBidExecutor.submit(() -> {
            // Tạo bản sao để tránh ConcurrentModificationException
            PriorityQueue<AutoBidConfig> snapshot = new PriorityQueue<>(queue);
            while (!snapshot.isEmpty()) {
                AutoBidConfig config = snapshot.poll(); // lấy theo đúng thứ tự ưu tiên
                if (config.getBidderId().equals(lastBid.getBidderId())) continue;
                double nextBid = lastBid.getAmount() + config.getIncrement();
                if (nextBid <= config.getMaxBid()) {
                    try {
                        auctionService.placeBid(auctionId, config.getBidderId(), nextBid);
                    } catch (Exception ignored) {}
                    break;
                }
            }
        });
    }

    @Override
    public void onAuctionEnd(String auctionId, String winnerId, double finalPrice) {
        autoBidMap.remove(auctionId);
    }
}
