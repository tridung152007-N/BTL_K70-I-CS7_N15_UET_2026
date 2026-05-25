package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionClientService;
import com.auction.client.session.UserSession;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuctionClientService service = new AuctionClientService();

    @FXML
    public void initialize() {
        SocketClient.getInstance().addListener(this::onMessage);
        errorLabel.setText("");
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                handleLogin();
            }
        });
        usernameField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                passwordField.requestFocus();
            }
        });
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        errorLabel.setText("Đang đăng nhập...");
        errorLabel.setStyle("-fx-text-fill: #3498db;");

        System.out.println("📤 Đang gửi login: " + username + " | pass length: " + password.length());
        service.login(username, password);
    }

    @FXML
    public void goToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/Register.fxml"));

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(loader.load(), 420, 500);

            stage.setScene(scene);
            stage.setTitle("Đăng ký tài khoản");
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Lỗi mở form đăng ký: " + e.getMessage());
        }
    }

    private void onMessage(Message msg) {
        Platform.runLater(() -> {
            System.out.println("🟢 LoginController nhận message: " + msg.getType());

            if (msg.getType() == MessageType.SUCCESS) {
                try {
                    UserPayload user = JsonUtil.fromJson(msg.getPayload(), UserPayload.class);
                    if (user != null && user.id() != null) {
                        UserSession.getInstance().login(user.id(), user.username(), user.role());
                        System.out.println("✅ Đăng nhập thành công: " + user.username() + " - " + user.role());
                        navigateBasedOnRole();
                    }
                } catch (Exception e) {
                    errorLabel.setText("Lỗi parse user data");
                    e.printStackTrace();
                }
            } else if (msg.getType() == MessageType.ERROR) {
                try {
                    String err = JsonUtil.fromJson(msg.getPayload(), ErrorPayload.class).error();
                    errorLabel.setText("❌ " + err);
                } catch (Exception e) {
                    errorLabel.setText("Đăng nhập thất bại");
                }
            }
        });
    }

    private void navigateBasedOnRole() {
        String role = UserSession.getInstance().getRole();
        try {
            String fxmlPath;
            String title;
            double width = 1150, height = 720;

            if ("SELLER".equalsIgnoreCase(role)) {
                fxmlPath = "/com/auction/client/fxml/Seller.fxml";
                title = "Quản lý sản phẩm";
                width = 950; height = 650;
            } else if ("ADMIN".equalsIgnoreCase(role)) {
                fxmlPath = "/com/auction/client/fxml/Admin.fxml";
                title = "Admin - Duyệt sản phẩm";
                width = 950; height = 650;
            } else {
                fxmlPath = "/com/auction/client/fxml/AuctionDashboard.fxml";
                title = "Latest Auctions";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), width, height);

            // Lấy stage an toàn hơn
            Stage stage = (Stage) usernameField.getScene().getWindow();
            if (stage == null) {
                stage = (Stage) errorLabel.getScene().getWindow(); // fallback
            }

            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();

            System.out.println("✅ Chuyển màn hình thành công: " + title);

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Lỗi chuyển màn hình: " + e.getMessage());
        }
    }

    private record ErrorPayload(String error) {}
    private record UserPayload(String id, String username, String role) {}
}