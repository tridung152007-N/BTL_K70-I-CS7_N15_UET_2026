package com.auction.server.service;

import com.auction.server.model.AutoBidConfig;
import com.auction.server.model.BidTransaction;
import com.auction.server.observer.AuctionObserver;

import java.util.*;
import java.util.concurrent.PriorityQueue;

/**
 * Xử lý Auto-Bidding – ưu tiên theo thời gian đăng ký (FIFO).
 */
public class AutoBidService implements AuctionObserver {

    private final AuctionService auctionService;
    // auctionId → danh sách auto-bid (sắp xếp theo registeredAt)
    private final Map<String, PriorityQueue<AutoBidConfig>> autoBidMap = new HashMap<>();

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

        for (AutoBidConfig config : queue) {
            if (config.getBidderId().equals(lastBid.getBidderId())) continue;
            double nextBid = lastBid.getAmount() + config.getIncrement();
            if (nextBid <= config.getMaxBid()) {
                auctionService.placeBid(auctionId, config.getBidderId(), nextBid);
                break;
            }
        }
    }

    @Override
    public void onAuctionEnd(String auctionId, String winnerId, double finalPrice) {
        autoBidMap.remove(auctionId);
    }
}
