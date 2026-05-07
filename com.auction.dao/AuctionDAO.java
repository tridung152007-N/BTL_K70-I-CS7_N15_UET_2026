package com.auction.dao;

import com.auction.model.Auction;
import java.sql.*;

public class AuctionDAO {

    public void save(Auction auction) throws SQLException {
        String sql = "INSERT INTO auctions (id, item_id, start_time, end_time, current_price, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auction.getId());
            stmt.setString(2, auction.getItem().getId());
            stmt.setTimestamp(3, Timestamp.valueOf(auction.getItem().getCreatedAt()));
            stmt.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));
            stmt.setDouble(5, auction.getCurrentPrice());
            stmt.setString(6, auction.getStatus().toString());
            stmt.executeUpdate();
        }
    }

    public void updatePrice(String auctionId, double newPrice, String highestBidderId) throws SQLException {
        String sql = "UPDATE auctions SET current_price=?, highest_bidder_id=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newPrice);
            stmt.setString(2, highestBidderId);
            stmt.setString(3, auctionId);
            stmt.executeUpdate();
        }
    }

    public void findById(String id) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("Tìm thấy auction: " + rs.getString("id"));
            }
        }
    }
}
