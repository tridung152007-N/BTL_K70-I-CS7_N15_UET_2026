package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionClientService;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField usernameField, emailField, shopNameField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private ComboBox<String> roleBox;
    @FXML private Label statusLabel;

    private final AuctionClientService service = new AuctionClientService();

    @FXML
    public void initialize() {
        SocketClient.getInstance().addListener(this::onMessage);

        roleBox.setItems(FXCollections.observableArrayList("BIDDER", "SELLER"));
        roleBox.setValue("BIDDER");

        roleBox.setOnAction(e -> {
            boolean isSeller = "SELLER".equals(roleBox.getValue());
            shopNameField.setVisible(isSeller);
            shopNameField.setManaged(isSeller);
        });
    }

    @FXML
    public void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();
        String email    = emailField.getText().trim();
        String role     = roleBox.getValue();
        String shopName = shopNameField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            setStatus("Vui lòng điền đầy đủ thông tin!", false);
            return;
        }
        if (!password.equals(confirm)) {
            setStatus("Mật khẩu xác nhận không khớp!", false);
            return;
        }
        if (password.length() < 6) {
            setStatus("Mật khẩu phải có ít nhất 6 ký tự!", false);
            return;
        }
        if ("SELLER".equals(role) && shopName.isEmpty()) {
            setStatus("Seller phải nhập tên cửa hàng!", false);
            return;
        }

        setStatus("Đang đăng ký...", true);

        // Gửi request đăng ký
        service.register(username, password, email, role, shopName);
    }

    private void onMessage(Message msg) {
        Platform.runLater(() -> {
            if (msg.getType() == MessageType.SUCCESS) {
                setStatus("✅ Đăng ký thành công! Vui lòng đăng nhập.", true);
                // Tự động quay về login sau 1.5s
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (Exception ignored) {}
                    Platform.runLater(this::goBackToLogin);
                }).start();
            } else if (msg.getType() == MessageType.ERROR) {
                String err = JsonUtil.fromJson(msg.getPayload(), ErrorPayload.class).error();
                setStatus("❌ " + err, false);
            }
        });
    }

    @FXML
    public void goBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/Login.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(loader.load(), 400, 300);
            stage.setScene(scene);
            stage.setTitle("Đăng nhập - Hệ thống đấu giá");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (success ? "green" : "red") + ";");
    }

    private record ErrorPayload(String error) {}
}
