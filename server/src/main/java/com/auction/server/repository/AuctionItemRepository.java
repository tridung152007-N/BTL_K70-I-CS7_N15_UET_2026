package com.auction.server.repository;

import com.auction.server.model.AuctionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long> {

    List<AuctionItem> findByActiveTrue();
}