package com.auction.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Ứng dụng chính cho Hệ thống Đấu giá Trực tuyến
 * Khởi tạo cửa sổ chính và quản lý các Scene
 */
public class AuctionApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Cài đặt cửa sổ
        stage.setTitle("Hệ thống Đấu giá Trực tuyến");
        stage.setWidth(1200);
        stage.setHeight(700);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);

        // Tải icon (nếu có)
        try {
            stage.getIcons().add(new Image(getClass().getResource("/images/icon.png").toString()));
        } catch (Exception e) {
            // Icon không bắt buộc
        }

        // Hiển thị màn hình đăng nhập
        showLoginScreen();

        stage.show();
    }

    /**
     * Hiển thị màn hình đăng nhập
     */
    public static void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AuctionApp.class.getResource("/fxml/LoginScreen.fxml")
            );
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hiển thị màn hình chính (danh sách đấu giá)
     */
    public static void showMainScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AuctionApp.class.getResource("/fxml/MainScreen.fxml")
            );
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hiển thị chi tiết sản phẩm
     */
    public static void showAuctionDetailScreen(int auctionId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AuctionApp.class.getResource("/fxml/AuctionDetailScreen.fxml")
            );
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hiển thị màn hình quản lý sản phẩm cho Seller
     */
    public static void showSellerDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    AuctionApp.class.getResource("/fxml/SellerDashboard.fxml")
            );
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}