package com.auction.client.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;

public class AuctionDashboardController {

    @FXML private TilePane auctionGrid;
    @FXML private TextField searchField;
    @FXML private Label emptyStateLabel;

    private final AuctionClientService service = new AuctionClientService();
    private List<Auction> allAuctions;
    private String currentUserId;
    
    // For paginated AUCTION_LIST accumulation
    private List<Auction> paginatedBuffer = new java.util.ArrayList<>();
    private java.util.Timer paginationTimer;
    private static final long PAGINATION_TIMEOUT_MS = 500;

    @FXML
    public void initialize() {
        currentUserId = UserSession.getInstance().getUserId();
        System.out.println("👤 AuctionDashboard init userId=" + currentUserId);

        boolean connected = SocketClient.getInstance().ensureConnected();
        System.out.println("🔌 AuctionDashboard STOMP connected=" + connected);
        SocketClient.getInstance().addListener(this::onMessage);

        Platform.runLater(() -> {
            System.out.println("🔌 AuctionDashboard: starting refresh");
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
        System.out.println("🎯 AuctionDashboard.onMessage type=" + msg.getType() + " sender=" + msg.getSenderId());
        if (msg.getType() == MessageType.AUCTION_LIST) {
            System.out.println("🎯 AuctionDashboard received AUCTION_LIST payload length=" + (msg.getPayload() != null ? msg.getPayload().length() : 0));
            try {
                // Cancel previous pagination timer
                if (paginationTimer != null) {
                    paginationTimer.cancel();
                }
                
                // Parse this page of auctions
                Auction[] auctionsArray = JsonUtil.fromJson(msg.getPayload(), Auction[].class);
                if (auctionsArray != null) {
                    System.out.println("  📄 Page contains " + auctionsArray.length + " auctions, buffer now has " + (paginatedBuffer.size() + auctionsArray.length));
                    paginatedBuffer.addAll(Arrays.asList(auctionsArray));
                }
                
                // Start/reset pagination timer - if no new page arrives in 500ms, consider pagination complete
                paginationTimer = new java.util.Timer();
                paginationTimer.schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("  📄 Pagination complete: " + paginatedBuffer.size() + " total auctions received");
                        allAuctions = new java.util.ArrayList<>(paginatedBuffer);
                        paginatedBuffer.clear();
                        
                        // Sort: còn thời gian lên đầu, đã kết thúc xuống dưới
                        allAuctions = allAuctions.stream()
                                .sorted((a, b) -> {
                                    LocalDateTime now = LocalDateTime.now();
                                    boolean aActive = a.getEndTime() != null && a.getEndTime().isAfter(now);
                                    boolean bActive = b.getEndTime() != null && b.getEndTime().isAfter(now);
                                    if (aActive && !bActive) return -1;
                                    if (!aActive && bActive) return 1;
                                    if (a.getEndTime() == null) return 1;
                                    if (b.getEndTime() == null) return -1;
                                    return a.getEndTime().compareTo(b.getEndTime());
                                })
                                .collect(java.util.stream.Collectors.toList());

                        Platform.runLater(() -> refreshGrid());
                    }
                }, PAGINATION_TIMEOUT_MS);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Tối ưu refreshGrid - chỉ refresh khi có dữ liệu mới
    private void refreshGrid() {
        if (auctionGrid == null) return;

        Platform.runLater(() -> {
            boolean noData = allAuctions == null || allAuctions.isEmpty();
            System.out.println("🔄 refreshGrid() - auctions=" + (allAuctions == null ? "null" : allAuctions.size()));
            auctionGrid.getChildren().clear();
            emptyStateLabel.setVisible(noData);
            if (noData) {
                return;
            }

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
