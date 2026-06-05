package com.auction.server.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConfig {

    private static final String URL      = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "12345";

    private DBConfig() {}

    // Trả về connection mới mỗi lần gọi — dùng try-with-resources để tự đóng
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Không tìm thấy MySQL Driver", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}