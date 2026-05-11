package com.auction.server.dao;

import com.auction.server.model.Item;
import java.util.List;
import java.util.Optional;

public interface ItemDAO {
    void save(Item item);
    Optional<Item> findById(String id);
    List<Item> findBySellerId(String sellerId);
    List<Item> findAll();
    void update(Item item);
    void delete(String id);
}
