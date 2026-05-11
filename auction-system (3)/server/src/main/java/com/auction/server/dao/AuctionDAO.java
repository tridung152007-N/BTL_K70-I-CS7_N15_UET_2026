package com.auction.server.dao;

import com.auction.common.model.AuctionState;
import com.auction.server.model.Auction;
import java.util.List;
import java.util.Optional;

public interface AuctionDAO {
    void save(Auction auction);
    Optional<Auction> findById(String id);
    List<Auction> findByState(AuctionState state);
    List<Auction> findAll();
    void update(Auction auction);
    void delete(String id);
}
