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

import java.time.LocalDateTime;
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
    private volatile boolean isActive = true;

    private ScheduledExecutorService autoRefreshService;
    private long lastUpdateTime = 0;
    private static final long UPDATE_THROTTLE_MS = 1500; // 1.5 giây

    @FXML
    public void initialize() {
        currentUserId = UserSession.getInstance().getUserId();
        String userName = UserSession.getInstance().getUsername();

        setupUserMenu(userName);
        SocketClient.getInstance().addListener(this::onMessage);

        Platform.runLater(() -> {
            handleRefresh();
            startAutoRefresh();
        });
    }

    private void setupUserMenu(String userName) {
        userMenu.getItems().addAll(
                "👤 " + userName,
                "📋 Hồ sơ",
                "🔔 Thông báo",
                "⚙️ Cài đặt",
                "🚪 Đăng xuất"
        );
        userMenu.setValue("👤 " + userName);
        userMenu.setOnAction(e -> handleUserMenuSelection());
    }

    private void onMessage(Message msg) {
        if (!isActive) return;

        if (msg.getType() == MessageType.AUCTION_LIST) {
            handleAuctionListUpdate(msg);
        }
        // Có thể xử lý BID_UPDATE nhẹ ở đây nếu muốn update card realtime
    }

    private void handleAuctionListUpdate(Message msg) {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < UPDATE_THROTTLE_MS) {
            return; // Throttle
        }
        lastUpdateTime = now;

        try {
            Auction[] auctionsArray = JsonUtil.fromJson(msg.getPayload(), Auction[].class);
            List<Auction> auctions = Arrays.asList(auctionsArray);

            Platform.runLater(() -> loadAuctionCards(auctions));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAuctionCards(List<Auction> auctions) {
        cardsContainer.getChildren().clear();

        for (Auction auction : auctions) {
            try {
                // Chỉ tạo card nếu auction còn đang diễn ra
                if (auction.getEndTime() != null && auction.getEndTime().isAfter(LocalDateTime.now())) {
                    AuctionCard card = new AuctionCard(auction, () -> openBidRoom(auction));
                    cardsContainer.getChildren().add(card);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleRefresh() {
        if (service != null && isActive) {
            service.requestAuctionList(currentUserId);
        }
    }

    private void startAutoRefresh() {
        autoRefreshService = Executors.newSingleThreadScheduledExecutor();
        // Giảm tần suất auto refresh xuống còn 3-5 phút
        autoRefreshService.scheduleAtFixedRate(this::handleRefresh, 60, 180, TimeUnit.SECONDS);
    }

    private void handleUserMenuSelection() {
        String selected = userMenu.getValue();
        if (selected == null) return;

        if (selected.contains("Đăng xuất")) {
            logout();
        } else if (selected.contains("Hồ sơ")) {
            openProfile();
        }
        // Reset value
        userMenu.setValue("👤 " + UserSession.getInstance().getUsername());
    }

    private void logout() {
        cleanup();
        // ... code logout hiện tại của bạn
    }

    private void openProfile() {
        // ... code mở profile
    }

    private void openBidRoom(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/BidRoom.fxml"));

            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            Scene scene = new Scene(loader.load(), 950, 680);

            BidRoomController ctrl = loader.getController();
            ctrl.setAuction(
                    auction.getId(),
                    auction.getItemName() != null ? auction.getItemName() : "Sản phẩm #" + auction.getId(),
                    auction.getEndTime()
            );
            ctrl.setCurrentUserId(currentUserId);

            stage.setScene(scene);
            stage.setTitle("Đấu giá - " + auction.getItemName());

            // Tắt refresh khi vào phòng đấu giá
            setActive(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void cleanup() {
        isActive = false;
        if (autoRefreshService != null && !autoRefreshService.isShutdown()) {
            autoRefreshService.shutdownNow();
        }
        SocketClient.getInstance().removeListener(this::onMessage);
    }
}