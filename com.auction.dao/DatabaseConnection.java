package com.auction.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    static {
        try (InputStream input = DatabaseConnection.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            URL = prop.getProperty("db.url");
            USER = prop.getProperty("db.user");
            PASSWORD = prop.getProperty("db.password");
        } catch (Exception e) {
            // fallback mặc định nếu không có file config
            URL = "jdbc:mysql://localhost:3306/auction_db";
            USER = "root";
            PASSWORD = "123456";
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
