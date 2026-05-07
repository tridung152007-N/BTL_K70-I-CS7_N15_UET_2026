package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.auction.app.AuctionApp;
import com.auction.service.UserService;

/**
 * Controller cho màn hình đăng nhập
 * Xử lý authentication thông qua UserService
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private CheckBox rememberMeCheckbox;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    private UserService userService = UserService.getInstance();

    @FXML
    public void initialize() {
        // Set default role
        roleComboBox.setValue("Người mua (Bidder)");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("⚠️ Vui lòng nhập tên đăng nhập và mật khẩu!");
            return;
        }

        try {
            // Authenticate user
            UserService.UserDTO user = userService.login(username, password);

            // Clear error message
            errorLabel.setText("");

            // Navigate according to user role
            if (UserService.ROLE_SELLER.equals(user.getRole())) {
                AuctionApp.showSellerDashboard();
            } else if (UserService.ROLE_ADMIN.equals(user.getRole())) {
                // TODO: Show admin panel
                AuctionApp.showMainScreen();
            } else {
                AuctionApp.showMainScreen();
            }

        } catch (IllegalArgumentException e) {
            errorLabel.setText("❌ " + e.getMessage());
        } catch (Exception e) {
            errorLabel.setText("❌ Đăng nhập thất bại: " + e.getMessage());
        }
    }

    @FXML
    private void handleRegisterClick() {
        // TODO: Show registration screen
        errorLabel.setText("ℹ️ Chức năng đăng ký sẽ được mở sớm!");
    }
}