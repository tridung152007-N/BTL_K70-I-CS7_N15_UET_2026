package com.auction.server.dao.impl;

import com.auction.server.dao.UserDAO;
import com.auction.server.model.*;
import com.auction.server.util.DBConfig;

import java.sql.*;
import java.util.*;

public class UserDAOImpl implements UserDAO {

    @Override
    public void save(User user) {
        String sql = "INSERT INTO users (id, username, password_hash, email, role, balance, shop_name, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getRole());
            ps.setDouble(6, user instanceof Bidder ? ((Bidder) user).getBalance() : 0);
            ps.setString(7, user instanceof Seller ? ((Seller) user).getShopName() : null);
            ps.setLong(8, user.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm user theo id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm user theo username: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DBConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách user: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET username=?, email=?, balance=?, shop_name=? WHERE id=?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setDouble(3, user instanceof Bidder ? ((Bidder) user).getBalance() : 0);
            ps.setString(4, user instanceof Seller ? ((Seller) user).getShopName() : null);
            ps.setString(5, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật user: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa user: " + e.getMessage(), e);
        }
    }

    // Chuyển 1 dòng SQL → đúng subclass User
    private User mapRow(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        return switch (role) {
            case "BIDDER" -> {
                Bidder b = new Bidder(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("email"));
                b.setBalance(rs.getDouble("balance"));
                yield b;
            }
            case "SELLER" -> {
                Seller s = new Seller(
                        rs.getString("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("email"),
                        rs.getString("shop_name"));
                yield s;
            }
            default -> new Admin(
                    rs.getString("id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("email"));
        };
    }
}
