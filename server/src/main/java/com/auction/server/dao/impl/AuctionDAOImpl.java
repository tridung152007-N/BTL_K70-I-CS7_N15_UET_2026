package com.auction.server.dao.impl;

import com.auction.common.model.AuctionState;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.model.Auction;
import com.auction.server.util.DBConfig;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
@Repository
public class AuctionDAOImpl implements AuctionDAO {

    @Override
    public void save(Auction auction) {
        String sql = """
            INSERT INTO auctions
              (id, item_id, seller_id, starting_price, current_price,
               current_leader_id, state, start_time, end_time, created_at, is_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getId());
            ps.setString(2, auction.getItemId());
            ps.setString(3, auction.getSellerId());
            ps.setDouble(4, auction.getStartingPrice());
            ps.setDouble(5, auction.getCurrentPrice());
            ps.setString(6, auction.getCurrentLeaderId());
            ps.setString(7, auction.getState().name());
            ps.setObject(8, auction.getStartTime());
            ps.setObject(9, auction.getEndTime());
            ps.setLong(10, auction.getCreatedAt());
            ps.setBoolean(11, auction.isPaid());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu auction: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Auction> findById(String id) {
        String sql = """
        SELECT a.*, i.name as item_name, i.image_path as item_image_path, u.username as current_leader_name
        FROM auctions a
        LEFT JOIN items i ON a.item_id = i.id
        LEFT JOIN users u ON a.current_leader_id = u.id
        WHERE a.id = ?
        """;
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm auction: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Auction> findByState(AuctionState state) {
        List<Auction> list = new ArrayList<>();
        String sql = """
        SELECT a.*, i.name as item_name, i.image_path as item_image_path, u.username as current_leader_name
        FROM auctions a
        LEFT JOIN items i ON a.item_id = i.id
        LEFT JOIN users u ON a.current_leader_id = u.id
        WHERE a.state = ?
        """;
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, state.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm auction theo state: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Auction> findAll() {
        List<Auction> list = new ArrayList<>();
        String sql = """
        SELECT a.*, i.name as item_name, i.image_path as item_image_path, u.username as current_leader_name
        FROM auctions a
        LEFT JOIN items i ON a.item_id = i.id
        LEFT JOIN users u ON a.current_leader_id = u.id
        ORDER BY a.created_at DESC
        """;
        try (Connection conn = DBConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách auction: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void update(Auction auction) {
        String sql = """
            UPDATE auctions SET
              current_price = ?, current_leader_id = ?,
              state = ?, end_time = ?, is_paid = ?
            WHERE id = ?
            """;
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, auction.getCurrentPrice());
            ps.setString(2, auction.getCurrentLeaderId());
            ps.setString(3, auction.getState().name());
            ps.setObject(4, auction.getEndTime());
            ps.setBoolean(5, auction.isPaid());
            ps.setString(6, auction.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật auction: " + e.getMessage(), e);
        }
    }

    public void markPaid(String auctionId) {
        String sql = "UPDATE auctions SET is_paid = TRUE WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auctionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật thanh toán auction: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM auctions WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa auction: " + e.getMessage(), e);
        }
    }

    private Auction mapRow(ResultSet rs) throws SQLException {
        Auction a = new Auction();
        a.setId(rs.getString("id"));
        a.setItemId(rs.getString("item_id"));
        a.setItemName(rs.getString("item_name"));
        a.setItemImagePath(rs.getString("item_image_path"));
        a.setSellerId(rs.getString("seller_id"));
        a.setStartingPrice(rs.getDouble("starting_price"));
        a.setCurrentPrice(rs.getDouble("current_price"));
        a.setCurrentLeaderId(rs.getString("current_leader_id"));
        a.setCurrentLeaderName(rs.getString("current_leader_name"));
        try {
            a.setPaid(rs.getBoolean("is_paid"));
        } catch (SQLException ignored) {
            a.setPaid(false);
        }
        a.setState(AuctionState.valueOf(rs.getString("state")));
        a.setStartTime(rs.getObject("start_time", LocalDateTime.class));
        a.setEndTime(rs.getObject("end_time", LocalDateTime.class));
        return a;
    }
}
