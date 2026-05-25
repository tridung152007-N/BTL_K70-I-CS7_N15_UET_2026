package com.auction.client.controller;

import com.auction.client.session.UserSession;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton;
import javafx.stage.Stage;

public class ProfileController {

    @FXML private MenuButton profileMenuButton;

    @FXML
    public void initialize() {
        String username = UserSession.getInstance().getUsername();
        if (username != null && !username.isEmpty()) {
            profileMenuButton.setText(username);
        }
    }

    @FXML
    public void showProfileInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông tin cá nhân");
        alert.setHeaderText("Thông tin tài khoản");
        alert.setContentText("Tên: " + UserSession.getInstance().getUsername() +
                "\nVai trò: " + UserSession.getInstance().getRole() +
                "\nID: " + UserSession.getInstance().getUserId());
        alert.showAndWait();
    }

    @FXML
    public void showActivityLog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Nhật ký hoạt động");
        alert.setHeaderText("Nhật ký gần đây");
        alert.setContentText("Chức năng đang phát triển...");
        alert.showAndWait();
    }

    @FXML
    public void logout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc muốn đăng xuất?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                UserSession.getInstance().logout();
                goToLogin();
            }
        });
    }

    private void goToLogin() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/Login.fxml"));
            Stage stage = (Stage) profileMenuButton.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(loader.load(), 420, 380));
            stage.setTitle("Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}