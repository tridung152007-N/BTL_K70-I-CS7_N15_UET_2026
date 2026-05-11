package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionClientService;
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

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuctionClientService service = new AuctionClientService();
    // FIX Bug 4: flag để chỉ xử lý response ngay sau khi gửi login
    private volatile boolean waitingForLoginResponse = false;

    @FXML
    public void initialize() {
        // Lắng nghe phản hồi từ server
        SocketClient.getInstance().addListener(this::onMessage);
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin.");
            return;
        }
        errorLabel.setText("Đang đăng nhập...");
        // Gửi mật khẩu dạng plain (nên hash BCrypt ở production)
        waitingForLoginResponse = true;
        service.login(username, password);
    }

    private void onMessage(Message msg) {
        // FIX Bug 4: chỉ xử lý khi đang chờ phản hồi login
        if (!waitingForLoginResponse) return;
        if (msg.getType() == MessageType.SUCCESS) {
            waitingForLoginResponse = false;
            SocketClient.getInstance().removeListener(this::onMessage);
            Platform.runLater(this::goToAuctionList);
        } else if (msg.getType() == MessageType.ERROR) {
            waitingForLoginResponse = false;
            String err = JsonUtil.fromJson(msg.getPayload(), ErrorPayload.class).error();
            Platform.runLater(() -> errorLabel.setText(err));
        }
    }

    private void goToAuctionList() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/AuctionList.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 800, 600));
            stage.setTitle("Danh sách đấu giá");
        } catch (Exception e) {
            errorLabel.setText("Lỗi chuyển màn hình: " + e.getMessage());
        }
    }

    private record ErrorPayload(String error) {}
}
