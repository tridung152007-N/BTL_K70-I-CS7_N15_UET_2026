package com.auction.server.dao;

import com.auction.server.model.BidTransaction;
import java.util.List;

public interface BidTransactionDAO {
    void save(BidTransaction bid);
    List<BidTransaction> findByAuctionId(String auctionId);
    List<BidTransaction> findByBidderId(String bidderId);
}
