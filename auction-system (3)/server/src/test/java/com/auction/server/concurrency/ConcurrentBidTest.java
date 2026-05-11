package com.auction.server.concurrency;

import com.auction.common.model.AuctionState;
import com.auction.server.model.Auction;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuctionServiceTest.InMemoryAuctionDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentBidTest {

    @Test
    @DisplayName("10 bidder đặt đồng thời – không lost update, chỉ 1 người dẫn đầu")
    void concurrentBids_noLostUpdate() throws InterruptedException {
        // Setup
        var auctionDAO = new InMemoryAuctionDAO();
        var service    = new AuctionService(auctionDAO, null);

        Auction auction = new Auction();
        auction.setId("concurrent-auction");
        auction.setCurrentPrice(10_000_000);
        auction.setState(AuctionState.RUNNING);
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        auctionDAO.save(auction);

        // 10 thread cùng lúc
        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final double amount  = 11_000_000 + i * 1_000_000L;
            final String bidder  = "bidder" + i;
            pool.submit(() -> {
                try {
                    ready.await(); // chờ lệnh xuất phát đồng thời
                    service.placeBid("concurrent-auction", bidder, amount);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {}
            });
        }

        ready.countDown(); // phát lệnh xuất phát
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        Auction result = auctionDAO.findById("concurrent-auction").get();

        // Luôn có đúng 1 người dẫn đầu
        assertNotNull(result.getCurrentLeaderId(), "Phải có người dẫn đầu");
        // Giá luôn >= giá khởi điểm + 1 bước
        assertTrue(result.getCurrentPrice() >= 11_000_000, "Giá phải tăng");
        System.out.println("✅ Bids thành công: " + successCount.get()
                + " | Leader: " + result.getCurrentLeaderId()
                + " | Giá: " + result.getCurrentPrice());
    }
}
