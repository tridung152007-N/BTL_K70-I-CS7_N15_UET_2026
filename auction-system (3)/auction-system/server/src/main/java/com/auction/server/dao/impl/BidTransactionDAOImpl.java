package com.auction.server.dao.impl;

import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.model.BidTransaction;
import com.auction.server.util.DBConfig;

import java.sql.*;
import java.util.*;

public class BidTransactionDAOImpl implements BidTransactionDAO {

    @Override
    public void save(BidTransaction bid) {
        String sql = "INSERT INTO bid_transactions (id, auction_id, bidder_id, amount, is_auto_bid, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bid.getId());
            ps.setString(2, bid.getAuctionId());
            ps.setString(3, bid.getBidderId());
            ps.setDouble(4, bid.getAmount());
            ps.setBoolean(5, bid.isAutoBid());
            ps.setLong(6, bid.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu bid: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BidTransaction> findByAuctionId(String auctionId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY created_at ASC";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm bid theo auction: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<BidTransaction> findByBidderId(String bidderId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE bidder_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bidderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm bid theo bidder: " + e.getMessage(), e);
        }
        return list;
    }

    private BidTransaction mapRow(ResultSet rs) throws SQLException {
        BidTransaction bid = new BidTransaction(
                rs.getString("auction_id"),
                rs.getString("bidder_id"),
                rs.getDouble("amount"),
                rs.getBoolean("is_auto_bid"));
        bid.setId(rs.getString("id"));
        return bid;
    }
}
