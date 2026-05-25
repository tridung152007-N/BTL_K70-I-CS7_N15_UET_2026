package com.auction.client.controller;

import com.auction.client.components.AuctionCard;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionClientService;
import com.auction.client.session.UserSession;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.Auction;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionListController {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField searchField;
    @FXML private Circle profileAvatar;
    @FXML private ComboBox<String> userMenu;

    private final AuctionClientService service = new AuctionClientService();
    private String currentUserId;

    @FXML
    public void initialize() {
        currentUserId = UserSession.getInstance().getUserId();
        String userName = UserSession.getInstance().getUsername();

        // Setup profile menu
        userMenu.getItems().addAll(
                "👤 " + userName,
                "📋 Hồ sơ",
                "🔔 Thông báo",
                "⚙️ Cài đặt",
                "🚪 Đăng xuất"
        );
        userMenu.setValue("👤 " + userName);
        userMenu.setOnAction(e -> handleUserMenuSelection());

        SocketClient.getInstance().addListener(this::onMessage);
        Platform.runLater(() -> {
            handleRefresh();
            startAutoRefresh();
        });
    }
    private ScheduledExecutorService autoRefreshService;

    private void startAutoRefresh() {
        autoRefreshService = Executors.newSingleThreadScheduledExecutor();
        autoRefreshService.scheduleAtFixedRate(this::handleRefresh, 45, 90, TimeUnit.SECONDS);
    }

    @FXML
    public void handleRefresh() {
        if (service != null) {
            service.requestAuctionList(currentUserId);
        }
    }

    // Thêm phương thức cleanup khi đóng cửa sổ
    public void cleanup() {
        if (autoRefreshService != null) {
            autoRefreshService.shutdownNow();
        }
        SocketClient.getInstance().removeListener(this::onMessage);
    }
    private void handleUserMenuSelection() {
        String selected = userMenu.getValue();
        if (selected == null) return;

        if (selected.contains("Đăng xuất")) {
            logout();
        } else if (selected.contains("Hồ sơ")) {
            openProfile();
        }
        userMenu.setValue("👤 " + UserSession.getInstance().getUsername());
    }

    private void logout() {
        try {
            UserSession.getInstance().logout();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/Login.fxml"));
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 400, 300));
            stage.setTitle("Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/ProfileMenu.fxml"));
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 500, 400));
            stage.setTitle("Hồ sơ người dùng");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onMessage(Message msg) {
        if (msg.getType() == MessageType.AUCTION_LIST) {
            try {
                Auction[] auctionsArray = JsonUtil.fromJson(msg.getPayload(), Auction[].class);
                List<Auction> auctions = Arrays.asList(auctionsArray);

                Platform.runLater(() -> loadAuctionCards(auctions));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadAuctionCards(List<Auction> auctions) {
        cardsContainer.getChildren().clear();

        for (Auction auction : auctions) {
            try {
                AuctionCard card = new AuctionCard(auction, () -> openBidRoom(auction));
                cardsContainer.getChildren().add(card);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openBidRoom(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/BidRoom.fxml"));

            // Sử dụng cardsContainer thay vì auctionGrid
            Stage stage = (Stage) cardsContainer.getScene().getWindow();

            Scene scene = new Scene(loader.load(), 950, 680);

            BidRoomController ctrl = loader.getController();

            // Truyền đầy đủ thông tin bao gồm endTime
            ctrl.setAuction(
                    auction.getId(),
                    auction.getItemName() != null ? auction.getItemName() : "Sản phẩm #" + auction.getId(),
                    auction.getEndTime()   // ← Quan trọng để đếm ngược
            );

            ctrl.setCurrentUserId(currentUserId);

            stage.setScene(scene);
            stage.setTitle("Đấu giá - " +
                    (auction.getItemName() != null ? auction.getItemName() : auction.getId()));

        } catch (Exception e) {
            e.printStackTrace();
            // Có thể thay bằng alert sau này
        }
    }
}
