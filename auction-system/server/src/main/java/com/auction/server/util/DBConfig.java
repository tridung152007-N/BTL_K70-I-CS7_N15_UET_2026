package com.auction.server.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton kết nối MySQL.
 * Đổi URL / USER / PASSWORD cho đúng máy của nhóm.
 */
public class DBConfig {

    private static final String URL      = "jdbc:mysql://localhost:3306/auction_db";
    private static final String USER     = "root";       // ← đổi thành user MySQL của bạn
    private static final String PASSWORD = "12345";     // ← đổi thành password MySQL của bạn

    private static Connection connection;

    private DBConfig() {}

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Kết nối MySQL thành công!");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Không tìm thấy MySQL Driver. Kiểm tra pom.xml!", e);
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kết nối MySQL: " + e.getMessage(), e);
        }
        return connection;
    }
}
