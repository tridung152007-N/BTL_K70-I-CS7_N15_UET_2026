package com.auction.server.dao.impl;

import com.auction.server.dao.ItemDAO;
import com.auction.server.factory.ItemFactory;
import com.auction.server.model.*;
import com.auction.server.util.DBConfig;

import java.sql.*;
import java.util.*;

public class ItemDAOImpl implements ItemDAO {

    @Override
    public void save(Item item) {
        String sql = """
            INSERT INTO items
              (id, name, description, category, seller_id,
               brand, warranty_months, artist, art_year,
               kilometre, manufacturing_year, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getCategory());
            ps.setString(5, item.getSellerId());
            // Electronics
            ps.setString(6, item instanceof Electronics ? ((Electronics) item).getBrand() : null);
            ps.setString(7, item instanceof Electronics ? ((Electronics) item).getWarrantyMonths() : null);
            // Art
            ps.setString(8, item instanceof Art ? ((Art) item).getArtist() : null);
            ps.setObject(9, item instanceof Art ? ((Art) item).getYear() : null);
            // Vehicle
            ps.setObject(10, item instanceof Vehicle ? ((Vehicle) item).getKilometre() : null);
            ps.setObject(11, item instanceof Vehicle ? ((Vehicle) item).getManufacturingYear() : null);
            ps.setLong(12, item.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu item: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Item> findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm item: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Item> findBySellerId(String sellerId) {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tìm item theo seller: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Item> findAll() {
        List<Item> list = new ArrayList<>();
        String sql = "SELECT * FROM items ORDER BY created_at DESC";
        try (Connection conn = DBConfig.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách item: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void update(Item item) {
        String sql = """
            UPDATE items SET name=?, description=?,
              brand=?, warranty_months=?, artist=?,
              art_year=?, kilometre=?, manufacturing_year=?
            WHERE id=?
            """;
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setString(3, item instanceof Electronics ? ((Electronics) item).getBrand() : null);
            ps.setString(4, item instanceof Electronics ? ((Electronics) item).getWarrantyMonths() : null);
            ps.setString(5, item instanceof Art ? ((Art) item).getArtist() : null);
            ps.setObject(6, item instanceof Art ? ((Art) item).getYear() : null);
            ps.setObject(7, item instanceof Vehicle ? ((Vehicle) item).getKilometre() : null);
            ps.setObject(8, item instanceof Vehicle ? ((Vehicle) item).getManufacturingYear() : null);
            ps.setString(9, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật item: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DBConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa item: " + e.getMessage(), e);
        }
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        String category = rs.getString("category");
        Item item = ItemFactory.createItem(category);
        item.setId(rs.getString("id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setSellerId(rs.getString("seller_id"));

        if (item instanceof Electronics e) {
            e.setBrand(rs.getString("brand"));
            e.setWarrantyMonths(rs.getString("warranty_months"));
        } else if (item instanceof Art a) {
            a.setArtist(rs.getString("artist"));
            a.setYear(rs.getInt("art_year"));
        } else if (item instanceof Vehicle v) {
            v.setKilometre(rs.getInt("kilometre"));
            v.setManufacturingYear(rs.getInt("manufacturing_year"));
        }
        return item;
    }
}
