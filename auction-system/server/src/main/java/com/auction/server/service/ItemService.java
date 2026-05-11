package com.auction.server.service;

import com.auction.server.dao.ItemDAO;
import com.auction.server.model.Item;

import java.util.List;

public class ItemService {
    private final ItemDAO itemDAO;

    public ItemService(ItemDAO itemDAO) { this.itemDAO = itemDAO; }

    public void addItem(Item item) {
        itemDAO.save(item);
    }

    public void updateItem(Item item) {
        itemDAO.findById(item.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + item.getId()));
        itemDAO.update(item);
    }

    public void deleteItem(String itemId) {
        itemDAO.delete(itemId);
    }

    public List<Item> getItemsBySeller(String sellerId) {
        return itemDAO.findBySellerId(sellerId);
    }

    public Item getById(String itemId) {
        return itemDAO.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + itemId));
    }
}
