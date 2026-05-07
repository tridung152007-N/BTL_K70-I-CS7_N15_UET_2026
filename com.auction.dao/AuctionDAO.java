package com.auction.dao;

import com.auction.model.Auction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    public void save(Auction auction) throws SQLException {
        String sql = "INSERT INTO auctions (id, item_id, start_time, end_time, current_price, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auction.getId());
            stmt.setString(2, auction.getItem().getId());
            stmt.setTimestamp(3, Timestamp.valueOf(auction.getStartTime())); // ✅ sửa lỗi
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

    public Auction findById(String id) throws SQLException { // ✅ sửa lỗi: trả về Auction
        String sql = "SELECT * FROM auctions WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Auction(
                    rs.getString("id"),
                    rs.getDouble("current_price"),
                    rs.getTimestamp("start_time").toLocalDateTime(),
                    rs.getTimestamp("end_time").toLocalDateTime()
                );
            }
        }
        return null;
    }
}
