package com.auction.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@jakarta.persistence.Entity
@Table(name = "auction_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuctionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private Double startingPrice;
    private Double currentPrice;
    private String seller;
    private LocalDateTime startTime;
    private boolean active = true;  // ← thêm dòng này

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Double getStartingPrice() {
        return startingPrice;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public String getSeller() {
        return seller;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public boolean isActive() {
        return active;
    }
}