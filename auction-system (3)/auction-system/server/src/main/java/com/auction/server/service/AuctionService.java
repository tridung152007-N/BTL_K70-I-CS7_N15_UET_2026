package com.auction.server.service;

import com.auction.common.exception.AuctionClosedException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.model.AuctionState;
import com.auction.common.util.Constants;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.model.Auction;
import com.auction.server.model.BidTransaction;
import com.auction.server.observer.AuctionObserver;
import com.auction.server.observer.AuctionSubject;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService implements AuctionSubject {

    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<AuctionObserver> observers = new ArrayList<>();

    public AuctionService(AuctionDAO auctionDAO, BidTransactionDAO bidTransactionDAO) {
        this.auctionDAO = auctionDAO;
        this.bidTransactionDAO = bidTransactionDAO;
    }

    /** Tạo phiên đấu giá mới */
    public void createAuction(Auction auction) {
        if (auction.getId() == null) auction.setId(UUID.randomUUID().toString());
        auction.setState(AuctionState.RUNNING);
        auctionDAO.save(auction);
    }

    /** Đặt giá – thread-safe, anti-sniping tích hợp */
    public BidTransaction placeBid(String auctionId, String bidderId, double amount) {
        lock.lock();
        try {
            Auction auction = auctionDAO.findById(auctionId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên: " + auctionId));

            if (auction.getState() != AuctionState.RUNNING)
                throw new AuctionClosedException("Phiên đấu giá đã đóng: " + auctionId);

            if (amount < auction.getCurrentPrice() + Constants.MIN_BID_INCREMENT)
                throw new InvalidBidException(String.format(
                        "Giá phải ≥ %.0f (hiện tại %.0f + bước %.0f)",
                        auction.getCurrentPrice() + Constants.MIN_BID_INCREMENT,
                        auction.getCurrentPrice(), Constants.MIN_BID_INCREMENT));

            BidTransaction bid = new BidTransaction(auctionId, bidderId, amount, false);
            bid.setId(UUID.randomUUID().toString());

            auction.setCurrentPrice(amount);
            auction.setCurrentLeaderId(bidderId);
            auction.addBid(bid);

            // Anti-sniping
            long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
            if (secondsLeft <= Constants.ANTI_SNIPE_WINDOW) {
                auction.setEndTime(auction.getEndTime().plusSeconds(Constants.ANTI_SNIPE_EXTEND));
                System.out.printf("⏱ Anti-snipe: gia hạn +%ds cho phiên %s%n",
                        Constants.ANTI_SNIPE_EXTEND, auctionId);
            }

            auctionDAO.update(auction);
            if (bidTransactionDAO != null) bidTransactionDAO.save(bid);
            notifyBidUpdate(auctionId, bid);
            return bid;
        } finally {
            lock.unlock();
        }
    }

    /** Đóng phiên – chuyển sang FINISHED */
    public void closeAuction(String auctionId) {
        lock.lock();
        try {
            Auction auction = auctionDAO.findById(auctionId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên: " + auctionId));
            auction.setState(AuctionState.FINISHED);
            auctionDAO.update(auction);
            notifyAuctionEnd(auctionId, auction.getCurrentLeaderId(), auction.getCurrentPrice());
        } finally {
            lock.unlock();
        }
    }

    public Auction getById(String auctionId) {
        return auctionDAO.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên: " + auctionId));
    }

    public List<Auction> getRunningAuctions() {
        return auctionDAO.findByState(AuctionState.RUNNING);
    }

    public List<Auction> getAllAuctions() {
        return auctionDAO.findAll();
    }

    // ── Observer ──────────────────────────────────────────
    @Override public void addObserver(AuctionObserver o) {
        synchronized (observers) { observers.add(o); }
    }
    @Override public void removeObserver(AuctionObserver o) {
        synchronized (observers) { observers.remove(o); }
    }
    @Override public void notifyBidUpdate(String auctionId, BidTransaction bid) {
        List<AuctionObserver> snap;
        synchronized (observers) { snap = new ArrayList<>(observers); }
        snap.forEach(o -> o.onBidUpdate(auctionId, bid));
    }
    @Override public void notifyAuctionEnd(String auctionId, String winnerId, double finalPrice) {
        List<AuctionObserver> snap;
        synchronized (observers) { snap = new ArrayList<>(observers); }
        snap.forEach(o -> o.onAuctionEnd(auctionId, winnerId, finalPrice));
    }
}
