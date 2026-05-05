package com.auction.factory;

import com.auction.model.Auction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Singleton quản lý toàn bộ phiên đấu giá
 */
public class AuctionManager {
    private static volatile AuctionManager instance;
    private final List<Auction> auctions = Collections.synchronizedList(new ArrayList<>());

    private AuctionManager() {}

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) instance = new AuctionManager();
            }
        }
        return instance;
    }

    public void addAuction(Auction a) { auctions.add(a); }

    public Auction findAuctionById(String id) {
        return auctions.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst().orElse(null);
    }

    public List<Auction> getAllAuctions() {
        return Collections.unmodifiableList(auctions);
    }
}