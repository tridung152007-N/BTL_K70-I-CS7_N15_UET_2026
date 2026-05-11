package com.auction.server.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Factory kết nối MySQL.
 * FIX Bug 5 & 6: Mỗi lần gọi getConnection() tạo một Connection MỚI thay vì
 * tái dùng singleton. Điều này an toàn với try-with-resources trong các DAO
 * (connection được đóng đúng cách sau mỗi query mà không ảnh hưởng đến các
 * thread khác) và tránh race condition trên môi trường multi-client.
 *
 * Lưu ý production: nên thay bằng HikariCP hoặc c3p0 connection pool.
 */
public class DBConfig {

    private static final String URL      = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String USER     = "root";       // ← đổi thành user MySQL của bạn
    private static final String PASSWORD = "123456";     // ← đổi thành password MySQL của bạn

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Không tìm thấy MySQL Driver. Kiểm tra pom.xml!", e);
        }
    }

    private DBConfig() {}

    /** Trả về một Connection MỚI mỗi lần gọi – caller chịu trách nhiệm đóng. */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kết nối MySQL: " + e.getMessage(), e);
        }
    }
}
