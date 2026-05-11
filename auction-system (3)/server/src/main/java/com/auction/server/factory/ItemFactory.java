package com.auction.server.factory;

import com.auction.server.model.*;

/** Factory Method – tạo Item theo category. */
public class ItemFactory {
    public static Item createItem(String category) {
        return switch (category.toUpperCase()) {
            case "ELECTRONICS" -> new Electronics();
            case "ART"         -> new Art();
            case "VEHICLE"     -> new Vehicle();
            default -> throw new IllegalArgumentException("Unknown category: " + category);
        };
    }
}
