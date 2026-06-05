package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionClientService;
import com.auction.client.session.UserSession;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

public class ProfileController {

    @FXML private MenuButton profileMenuButton;

    private final AuctionClientService service = new AuctionClientService();

    @FXML
    public void initialize() {
        try {
            String username = UserSession.getInstance().getUsername();
            if (username != null && !username.isEmpty()) {
                profileMenuButton.setText(username);
            }
            SocketClient.getInstance().addListener(this::onMessage);
            if (UserSession.getInstance().getUserId() != null) {
                service.requestWalletBalance(UserSession.getInstance().getUserId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showProfileInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thong tin ca nhan");
        alert.setHeaderText("Thong tin tai khoan");
        alert.setContentText("Ten: " + UserSession.getInstance().getUsername()
                + "\nVai tro: " + UserSession.getInstance().getRole()
                + "\nID: " + UserSession.getInstance().getUserId()
                + "\nSo du: " + String.format("%,.0f VND", UserSession.getInstance().getBalance()));
        alert.showAndWait();
    }

    @FXML
    public void topUpWallet() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nap tien");
        dialog.setHeaderText("Nap tien vao vi");
        dialog.setContentText("So tien (VND):");
        dialog.showAndWait().ifPresent(value -> {
            try {
                double amount = Double.parseDouble(value.trim());
                service.topUpWallet(UserSession.getInstance().getUserId(), amount);
            } catch (Exception e) {
                showAlert("Loi", "So tien khong hop le.");
            }
        });
    }

    @FXML
    public void showActivityLog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nhat ky hoat dong");
        alert.setHeaderText("Nhat ky gan day");
        alert.setContentText("Lich su dau gia duoc hien thi trong tung phong dau gia.");
        alert.showAndWait();
    }

    @FXML
    public void logout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Ban co chac muon dang xuat?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                SocketClient.getInstance().removeListener(this::onMessage);
                SocketClient.getInstance().disconnect();
                UserSession.getInstance().logout();
                goToLogin();
            }
        });
    }

    private void onMessage(Message msg) {
        if (msg.getType() != MessageType.WALLET_BALANCE) return;
        try {
            JsonObject obj = JsonParser.parseString(msg.getPayload()).getAsJsonObject();
            String userId = UserSession.getInstance().getUserId();
            if (userId == null || !userId.equals(obj.get("userId").getAsString())) return;
            double balance = obj.get("balance").getAsDouble();
            UserSession.getInstance().setBalance(balance);
            Platform.runLater(() -> profileMenuButton.setText(
                    UserSession.getInstance().getUsername() + " - " + String.format("%,.0f VND", balance)));
        } catch (Exception ignored) {
        }
    }

    private void goToLogin() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/Login.fxml"));
            Stage stage = (Stage) profileMenuButton.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(loader.load(), 420, 380));
            stage.setTitle("Dang nhap");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
