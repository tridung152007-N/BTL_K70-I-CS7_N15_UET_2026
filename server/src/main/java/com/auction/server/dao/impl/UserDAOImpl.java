package com.auction.server.dao.impl;

import com.auction.server.dao.UserDAO;
import com.auction.server.model.*;
import com.auction.server.util.DBConfig;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;
@Repository
public class UserDAOImpl implements UserDAO {

    @Override
    public void save(User user) {
        String sql = "INSERT INTO users (id, username, password_hash, email, role, balance, shop_name, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getRole());
            ps.setDouble(6, user instanceof Bidder ? ((Bidder) user).getBalance() : 0.0);
            ps.setString(7, user instanceof Seller ? ((Seller) user).getShopName() : null);
            ps.setLong(8, user.getCreatedAt());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);   // ← THÊM DÒNG NÀY

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {   // ← thêm dòng này

            ps.setString(1, username);   // ← THÊM DÒNG NÀY

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by username: " + e.getMessage(), e);
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

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving user list: " + e.getMessage(), e);
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
            ps.setDouble(3, user instanceof Bidder ? ((Bidder) user).getBalance() : 0.0);
            ps.setString(4, user instanceof Seller ? ((Seller) user).getShopName() : null);
            ps.setString(5, user.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating user: " + e.getMessage(), e);
        }
    }

    @Override
    public double addBalance(String userId, double amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, userId);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Không tìm thấy người dùng");
            }
            return findById(userId)
                    .filter(Bidder.class::isInstance)
                    .map(Bidder.class::cast)
                    .map(Bidder::getBalance)
                    .orElse(0.0);
        } catch (SQLException e) {
            throw new RuntimeException("Error adding balance: " + e.getMessage(), e);
        }
    }

    @Override
    public double subtractBalance(String userId, double amount) {
        String sql = "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, userId);
            ps.setDouble(3, amount);
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Số dư không đủ để thanh toán");
            }
            return findById(userId)
                    .filter(Bidder.class::isInstance)
                    .map(Bidder.class::cast)
                    .map(Bidder::getBalance)
                    .orElse(0.0);
        } catch (SQLException e) {
            throw new RuntimeException("Error subtracting balance: " + e.getMessage(), e);
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
            throw new RuntimeException("Error deleting user: " + e.getMessage(), e);
        }
    }

    // Map ResultSet to User object (không dùng yield)
    private User mapRow(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        String id = rs.getString("id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String email = rs.getString("email");

        switch (role) {
            case "BIDDER":
                Bidder bidder = new Bidder(id, username, passwordHash, email);
                bidder.setBalance(rs.getDouble("balance"));
                return bidder;

            case "SELLER":
                return new Seller(id, username, passwordHash, email, rs.getString("shop_name"));

            default: // ADMIN
                return new Admin(id, username, passwordHash, email);
        }
    }
}
