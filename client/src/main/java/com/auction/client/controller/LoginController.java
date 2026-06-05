package com.auction.client.controller;

import java.io.IOException;

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

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuctionClientService service = new AuctionClientService();
    private volatile boolean waitingForLogin = false;

    @FXML
    public void initialize() {
        SocketClient.getInstance().ensureConnected();
        SocketClient.getInstance().addListener(this::onMessage);
        errorLabel.setText("");
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) handleLogin();
        });
        usernameField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) passwordField.requestFocus();
        });
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui long nhap day du thong tin.");
            return;
        }

        System.out.println(" Attempting login: username=" + username);
        waitingForLogin = true;
        errorLabel.setText("Dang dang nhap...");
        errorLabel.setStyle("-fx-text-fill: #3498db;");
        if (!service.login(username, password)) {
            waitingForLogin = false;
            errorLabel.setStyle("-fx-text-fill: #e74c3c;");
            errorLabel.setText("Khong ket noi duoc server. Hay kiem tra server da chay chua.");
            return;
        }
        startLoginTimeout();
        System.out.println("ðŸ“¤ Login request sent, waiting for response...");
    }
    @FXML
    public void goToRegister() {
        try {
            SocketClient.getInstance().removeListener(this::onMessage);
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/Register.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 420, 500));
            stage.setTitle("Dang ky tai khoan");
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Loi mo form dang ky: " + describe(e));
        }
    }

    private void onMessage(Message msg) {
        System.out.println(" LoginController.onMessage() received: type=" + msg.getType() + ", waiting=" + waitingForLogin);

        if (!waitingForLogin) {
            System.out.println(" Not waiting for login, ignore message");
            return;
        }

        if (msg.getType() == MessageType.SUCCESS) {
            System.out.println(" SUCCESS message received, payload length=" + (msg.getPayload() != null ? msg.getPayload().length() : 0));
            System.out.println(" Payload: " + msg.getPayload());

            try {
                UserPayload user = JsonUtil.fromJson(msg.getPayload(), UserPayload.class);
                System.out.println(" Parsed user: id=" + (user != null ? user.id() : "null"));

                if (user == null || user.id() == null || user.role() == null) {
                    System.out.println(" User is null or missing fields");
                    return;
                }

                waitingForLogin = false;
                SocketClient.getInstance().removeListener(this::onMessage);
                UserSession.getInstance().login(user.id(), user.username(), user.role(), user.balance() != null ? user.balance().doubleValue() : 0);
                System.out.println(" UserSession logged in, navigating...");
                Platform.runLater(this::navigateBasedOnRole);
            } catch (Exception e) {
                System.err.println(" Error parsing SUCCESS message:");
                e.printStackTrace();
            }
        } else if (msg.getType() == MessageType.ERROR) {
            System.out.println(" ERROR message received: " + msg.getPayload());
            try {
                ErrorPayload err = JsonUtil.fromJson(msg.getPayload(), ErrorPayload.class);
                if (err == null || err.error() == null) return;
                String errMsg = err.error();
                if (!errMsg.contains("tim item") && !errMsg.contains("seller")
                        && !errMsg.contains("auction") && !errMsg.contains("item")) {
                    waitingForLogin = false;
                    Platform.runLater(() -> {
                        errorLabel.setStyle("-fx-text-fill: #e74c3c;");
                        errorLabel.setText("Loi: " + errMsg);
                    });
                }
            } catch (Exception e) {
                System.err.println(" Error parsing ERROR message: " + e.getMessage());
            }
        } else {
            System.out.println(" Unexpected message type: " + msg.getType());
        }
    }

    private void startLoginTimeout() {
        javafx.animation.PauseTransition timeout =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(8));
        timeout.setOnFinished(event -> {
            if (waitingForLogin) {
                waitingForLogin = false;
                errorLabel.setStyle("-fx-text-fill: #e74c3c;");
                errorLabel.setText("Khong nhan duoc phan hoi tu server. Hay chay lai server va client da build moi.");
            }
        });
        timeout.play();
    }

    private void navigateBasedOnRole() {
        String role = UserSession.getInstance().getRole();
        try {
            String fxmlPath;
            String title;
            double width = 1150;
            double height = 720;

            if ("SELLER".equalsIgnoreCase(role)) {
                fxmlPath = "/com/auction/client/fxml/Seller.fxml";
                title = "Quan ly san pham";
                width = 950;
                height = 650;
            } else if ("ADMIN".equalsIgnoreCase(role)) {
                fxmlPath = "/com/auction/client/fxml/Admin.fxml";
                title = "Admin - Duyet san pham";
                width = 950;
                height = 650;
            } else {
                fxmlPath = "/com/auction/client/fxml/AuctionDashboard.fxml";
                title = "Latest Auctions";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), width, height));
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setStyle("-fx-text-fill: #e74c3c;");
            errorLabel.setText("Loi chuyen man hinh: " + describe(e));
        }
    }

    private String describe(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName();
    }

    private record ErrorPayload(String error) {}
    private record UserPayload(String id, String username, String role, Double balance, String email, String passwordHash) {}
}
