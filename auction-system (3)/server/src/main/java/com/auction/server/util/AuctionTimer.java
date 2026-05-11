package com.auction.server.util;

import com.auction.server.service.AuctionService;

import java.util.Map;
import java.util.concurrent.*;

/** Quản lý tự động đóng phiên khi hết giờ. */
public class AuctionTimer {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final AuctionService auctionService;

    public AuctionTimer(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void schedule(String auctionId, long delaySeconds) {
        cancel(auctionId);
        ScheduledFuture<?> task = scheduler.schedule(
                () -> auctionService.closeAuction(auctionId),
                delaySeconds, TimeUnit.SECONDS);
        tasks.put(auctionId, task);
    }

    public void cancel(String auctionId) {
        ScheduledFuture<?> old = tasks.remove(auctionId);
        if (old != null) old.cancel(false);
    }
}
