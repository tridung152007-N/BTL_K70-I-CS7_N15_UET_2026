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
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionDashboardController {

    @FXML private TilePane auctionGrid;
    @FXML private TextField searchField;

    private final AuctionClientService service = new AuctionClientService();
    private List<Auction> allAuctions;
    private String currentUserId;

    @FXML
    public void initialize() {
        currentUserId = UserSession.getInstance().getUserId();

        // Đảm bảo chỉ add listener 1 lần
        SocketClient.getInstance().addListener(this::onMessage);

        Platform.runLater(() -> {
            handleRefresh();        // Load lần đầu
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

    private void onMessage(Message msg) {
        if (msg.getType() == MessageType.AUCTION_LIST) {
            try {
                Auction[] auctionsArray = JsonUtil.fromJson(msg.getPayload(), Auction[].class);
                allAuctions = Arrays.asList(auctionsArray);
                refreshGrid();   // Gọi refresh
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Tối ưu refreshGrid - chỉ refresh khi có dữ liệu mới
    private void refreshGrid() {
        if (auctionGrid == null || allAuctions == null) return;

        Platform.runLater(() -> {
            System.out.println("🔄 refreshGrid() - Số auction: " + allAuctions.size());
            auctionGrid.getChildren().clear();

            for (Auction auction : allAuctions) {
                try {
                    AuctionCard card = new AuctionCard(auction, () -> openBidRoom(auction));
                    auctionGrid.getChildren().add(card);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void filterAuctions(String keyword) {
        if (allAuctions == null) return;
        auctionGrid.getChildren().clear();

        String search = keyword.toLowerCase().trim();
        for (Auction auction : allAuctions) {
            String name = (auction.getItemName() != null ? auction.getItemName() : "").toLowerCase();
            if (search.isEmpty() || name.contains(search)) {
                try {
                    AuctionCard card = new AuctionCard(auction, () -> openBidRoom(auction));
                    auctionGrid.getChildren().add(card);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void openBidRoom(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/BidRoom.fxml"));

            Stage stage = (Stage) auctionGrid.getScene().getWindow(); // hoặc cardsContainer
            Scene scene = new Scene(loader.load(), 950, 680);

            BidRoomController ctrl = loader.getController();
            ctrl.setAuction(
                    auction.getId(),
                    auction.getItemName() != null ? auction.getItemName() : "Sản phẩm #" + auction.getId(),
                    auction.getEndTime()  // ← Quan trọng
            );
            ctrl.setCurrentUserId(currentUserId);

            stage.setScene(scene);
            stage.setTitle("Đấu giá - " + auction.getItemName());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
