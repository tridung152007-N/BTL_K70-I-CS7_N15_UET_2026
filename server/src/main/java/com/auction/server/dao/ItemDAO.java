package com.auction.server.dao;

import com.auction.server.model.Item;
import java.util.List;
import java.util.Optional;

public interface ItemDAO {
    void save(Item item);
    Optional<Item> findById(String id);
    List<Item> findBySellerId(String sellerId);
    List<Item> findByStatus(String status);
    List<Item> findAll();
    void update(Item item);
    void updateStatus(String itemId, String status);
    void delete(String id);
}
