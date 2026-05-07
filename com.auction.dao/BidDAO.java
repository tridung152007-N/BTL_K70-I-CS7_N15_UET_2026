package com.auction.dao;

import com.auction.model.BidTransaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    public void save(BidTransaction bid) throws SQLException {
        String sql = "INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bid.getId());
            stmt.setString(2, bid.getAuctionId());
            stmt.setString(3, bid.getBidderId());
            stmt.setDouble(4, bid.getBidAmount());
            stmt.executeUpdate();
        }
    }

    public List<BidTransaction> findByAuctionId(String auctionId) throws SQLException { // ✅ sửa lỗi: trả về List
        String sql = "SELECT * FROM bid_transactions WHERE auction_id=? ORDER BY bid_amount DESC";
        List<BidTransaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auctionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new BidTransaction(
                    rs.getString("id"),
                    rs.getString("auction_id"),
                    rs.getString("bidder_id"),
                    rs.getDouble("bid_amount")
                ));
            }
        }
        return list;
    }
}
