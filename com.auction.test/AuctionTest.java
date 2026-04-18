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
    private Item item;

    @BeforeEach
    void setUp() {
        // Tạo một item và một phiên đấu giá giả lập
        item = new Item("I1", "Laptop Dell XPS", 1000.0);
        // Thiết lập phiên đấu giá bắt đầu từ 1 phút trước và kết thúc sau 5 phút
        auction = new Auction("A1", item, 
            LocalDateTime.now().minusMinutes(1), 
            LocalDateTime.now().plusMinutes(5));
        
        // Cập nhật trạng thái để phiên trở thành RUNNING
        auction.updateStatus();
    }

    @Test
    @DisplayName("Test đặt giá hợp lệ")
    void testValidBid() {
        assertDoesNotThrow(() -> {
            auction.placeBid("User_01", 1100.0);
        });
        assertEquals(1100.0, auction.getCurrentPrice(), "Giá hiện tại phải được cập nhật");
        assertEquals("User_01", auction.getHighestBidderId(), "ID người thắng phải được cập nhật");
    }

    @Test
    @DisplayName("Test đặt giá thấp hơn giá hiện tại (Phải ném ngoại lệ)")
    void testInvalidLowerBid() {
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid("User_02", 900.0);
        }, "Phải ném InvalidBidException khi bid thấp hơn giá khởi điểm");
    }

    @Test
    @DisplayName("Test đặt giá khi phiên đã kết thúc")
    void testBidOnFinishedAuction() {
        // Giả lập phiên đã hết hạn
        Auction expiredAuction = new Auction("A2", item, 
            LocalDateTime.now().minusMinutes(10), 
            LocalDateTime.now().minusMinutes(5));
        expiredAuction.updateStatus();

        assertThrows(AuctionException.class, () -> {
            expiredAuction.placeBid("User_03", 2000.0);
        }, "Phải ném AuctionException khi đấu giá đã kết thúc");
    }

    @Test
    @DisplayName("Test xử lý đa luồng (10 người cùng bid một lúc)")
    void testConcurrentBidding() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // Giả lập 10 người cùng đặt giá tăng dần từ 1100, 1200... đến 2000
        for (int i = 1; i <= numberOfThreads; i++) {
            final double bidAmount = 1000.0 + (i * 100);
            executorService.execute(() -> {
                try {
                    auction.placeBid("User_" + bidAmount, bidAmount);
                } catch (Exception e) {
                    // Log lỗi nếu cần
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(2, TimeUnit.SECONDS);
        executorService.shutdown();

        // Kiểm tra xem giá cuối cùng có phải là giá cao nhất (2000) không
        // Điều này chứng minh ReentrantLock đã hoạt động tốt
        assertEquals(2000.0, auction.getCurrentPrice(), "Giá cuối cùng phải là 2000.0");
    }
}