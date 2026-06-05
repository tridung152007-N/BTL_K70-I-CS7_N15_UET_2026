package com.auction.server.service;

import com.auction.server.model.AuctionItem;
import com.auction.server.repository.AuctionItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuctionItemService {

    private final AuctionItemRepository repository;

    public AuctionItemService(AuctionItemRepository repository) {
        this.repository = repository;
    }

    public AuctionItem save(AuctionItem item) {
        return repository.save(item);
    }

    public List<AuctionItem> getAllActiveItems() {
        return repository.findByActiveTrue();
    }

    public AuctionItem findById(Long id) {
        return repository.findById(id).orElse(null);
    }
}