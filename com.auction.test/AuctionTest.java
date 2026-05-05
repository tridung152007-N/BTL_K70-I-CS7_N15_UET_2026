package com.auction.test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import com.auction.model.*;
import com.auction.exception.*;
import java.time.LocalDateTime;
import java.util.concurrent.*;

public class AuctionTest {
    private Auction auction;
    private Items item;

    @BeforeEach
    void setUp() {
        // Dùng Electronics thay vì Item (lớp abstract không thể khởi tạo trực tiếp)
        item = new Electronics("I1", "Laptop Dell XPS", "Laptop cao cấp", 1000.0, "seller1", 12);
        auction = new Auction("A1", item,
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().plusMinutes(5));
        auction.updateStatus();
    }

    @Test
    @DisplayName("Test đặt giá hợp lệ")
    void testValidBid() {
        assertDoesNotThrow(() -> auction.placeBid("User_01", 1100.0));
        assertEquals(1100.0, auction.getCurrentPrice(), "Giá hiện tại phải được cập nhật");
        assertEquals("User_01", auction.getHighestBidderId(), "ID người thắng phải được cập nhật");
    }

    @Test
    @DisplayName("Test đặt giá thấp hơn giá hiện tại (Phải ném ngoại lệ)")
    void testInvalidLowerBid() {
        assertThrows(InvalidBidException.class, () -> auction.placeBid("User_02", 900.0),
            "Phải ném InvalidBidException khi bid thấp hơn giá khởi điểm");
    }

    @Test
    @DisplayName("Test đặt giá khi phiên đã kết thúc")
    void testBidOnFinishedAuction() {
        Auction expiredAuction = new Auction("A2", item,
            LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now().minusMinutes(5));
        expiredAuction.updateStatus();

        assertThrows(AuctionException.class, () -> expiredAuction.placeBid("User_03", 2000.0),
            "Phải ném AuctionException khi đấu giá đã kết thúc");
    }

    @Test
    @DisplayName("Test xử lý đa luồng (10 người cùng bid một lúc)")
    void testConcurrentBidding() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 1; i <= numberOfThreads; i++) {
            final double bidAmount = 1000.0 + (i * 100);
            executorService.execute(() -> {
                try {
                    auction.placeBid("User_" + bidAmount, bidAmount);
                } catch (Exception e) {
                    // Các bid thấp hơn bid đang dẫn đầu sẽ bị reject — đó là hành vi đúng
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(2, TimeUnit.SECONDS);
        executorService.shutdown();

        // Giá cuối phải là giá cao nhất trong số các bid được chấp nhận
        assertTrue(auction.getCurrentPrice() >= 1100.0, "Giá phải cao hơn giá khởi điểm");
    }
}