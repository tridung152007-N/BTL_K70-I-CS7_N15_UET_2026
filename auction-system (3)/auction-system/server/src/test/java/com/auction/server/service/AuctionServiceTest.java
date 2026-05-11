package com.auction.server.service;

import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.model.AuctionState;
import com.auction.server.model.Auction;
import com.auction.server.model.BidTransaction;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AuctionServiceTest {

    // DAO giả (in-memory) để test không cần database
    private InMemoryAuctionDAO auctionDAO;
    private AuctionService service;

    @BeforeEach
    void setUp() {
        auctionDAO = new InMemoryAuctionDAO();
        service = new AuctionService(auctionDAO, null);

        // Tạo phiên đấu giá RUNNING mẫu
        Auction auction = new Auction();
        auction.setId("a1");
        auction.setStartingPrice(10_000_000);
        auction.setCurrentPrice(10_000_000);
        auction.setState(AuctionState.RUNNING);
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        auctionDAO.save(auction);
    }

    @Test
    @DisplayName("Bid hợp lệ – giá phải tăng đúng")
    void placeBid_valid_shouldUpdatePrice() {
        BidTransaction bid = service.placeBid("a1", "bidder1", 11_000_000);

        assertNotNull(bid);
        assertEquals(11_000_000, bid.getAmount());
        assertEquals(11_000_000, auctionDAO.findById("a1").get().getCurrentPrice());
        assertEquals("bidder1", auctionDAO.findById("a1").get().getCurrentLeaderId());
    }

    @Test
    @DisplayName("Bid thấp hơn bước giá tối thiểu – phải ném InvalidBidException")
    void placeBid_tooLow_shouldThrowInvalidBidException() {
        assertThrows(InvalidBidException.class, () ->
                service.placeBid("a1", "bidder1", 10_000_500)); // chưa đủ +1000
    }

    @Test
    @DisplayName("Bid vào phiên đã đóng – phải ném AuctionClosedException")
    void placeBid_closedAuction_shouldThrowAuctionClosedException() {
        // Đóng phiên trước
        service.closeAuction("a1");
        assertThrows(AuctionClosedException.class, () ->
                service.placeBid("a1", "bidder1", 20_000_000));
    }

    @Test
    @DisplayName("Đóng phiên – state phải chuyển sang FINISHED")
    void closeAuction_shouldSetStateFinished() {
        service.closeAuction("a1");
        assertEquals(AuctionState.FINISHED,
                auctionDAO.findById("a1").get().getState());
    }

    // ── In-memory DAO dùng cho test (không cần MySQL) ──────
    static class InMemoryAuctionDAO implements com.auction.server.dao.AuctionDAO {
        private final Map<String, Auction> store = new HashMap<>();

        @Override public void save(Auction a) { store.put(a.getId(), a); }
        @Override public Optional<Auction> findById(String id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Auction> findByState(AuctionState s) {
            return store.values().stream().filter(a -> a.getState() == s).toList();
        }
        @Override public List<Auction> findAll() { return new ArrayList<>(store.values()); }
        @Override public void update(Auction a) { store.put(a.getId(), a); }
        @Override public void delete(String id) { store.remove(id); }
    }
}
