package com.auction.client.controller;

import com.auction.client.chart.BidHistoryChart;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.AutoBidClientService;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.BidTransaction;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BidRoomController {

    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label timerLabel;
    @FXML private TextField bidAmountField;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private StackPane chartContainer;
    @FXML private TableView<BidTransaction> bidHistoryTable;
    @FXML private TableColumn<BidTransaction, String> colBidder;
    @FXML private TableColumn<BidTransaction, Double> colAmount;
    @FXML private TableColumn<BidTransaction, String> colTime;

    private final ObservableList<BidTransaction> bidList = FXCollections.observableArrayList();

    private final AuctionClientService auctionService = new AuctionClientService();
    private final AutoBidClientService autoBidService = new AutoBidClientService();
    private final BidHistoryChart bidHistoryChart;

    private String auctionId;
    private String currentUserId;
    private String itemName;
    private LocalDateTime endTime;
    private ScheduledExecutorService countdownExecutor;
    private volatile boolean isActive = true;
    private volatile boolean isEnded = false;

    private long lastUpdateTime = 0;
    private static final long UPDATE_THROTTLE_MS = 800;

    public BidRoomController() {
        this.bidHistoryChart = new BidHistoryChart();
    }

    @FXML
    public void initialize() {
        setupChart();
        setupTable();
        SocketClient.getInstance().addListener(this::onMessage);
    }

    private void setupChart() {
        if (chartContainer != null) {
            chartContainer.getChildren().clear();
            chartContainer.getChildren().add(bidHistoryChart.getChart());
        }
    }

    private void setupTable() {
        if (bidHistoryTable != null) {
            colBidder.setCellValueFactory(new PropertyValueFactory<>("bidderId"));
            colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
            colTime.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
            bidHistoryTable.setItems(bidList);
        }
    }

    public void setAuction(String auctionId, String itemName, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.itemName = itemName != null && !itemName.isEmpty() ? itemName : "Phiên #" + auctionId;
        this.endTime = endTime;

        if (itemNameLabel != null) {
            itemNameLabel.setText(this.itemName);
        }

        if (endTime != null) {
            startCountdown();
        }
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        // Không gọi setCurrentUserId trên chart nữa vì class chart chưa hỗ trợ
    }

    private void startCountdown() {
        if (countdownExecutor != null) return;

        countdownExecutor = Executors.newSingleThreadScheduledExecutor();
        countdownExecutor.scheduleAtFixedRate(this::updateCountdown, 0, 1, TimeUnit.SECONDS);
    }

    private void updateCountdown() {
        if (!isActive || endTime == null || isEnded) return;

        Platform.runLater(() -> {
            long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);

            if (secondsLeft <= 0) {
                auctionEnded();
                return;
            }

            long hours = secondsLeft / 3600;
            long minutes = (secondsLeft % 3600) / 60;
            long seconds = secondsLeft % 60;

            String timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            timerLabel.setText("⏱ " + timeStr + " còn lại");

            if (secondsLeft <= 60) {
                timerLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else if (secondsLeft <= 300) {
                timerLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
            } else {
                timerLabel.setStyle("-fx-text-fill: #27ae60;");
            }
        });
    }

    private void auctionEnded() {
        isEnded = true;
        cleanupTimer();

        Platform.runLater(() -> {
            timerLabel.setText("✅ Phiên đã kết thúc!");
            timerLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

            if (bidAmountField != null) bidAmountField.setDisable(true);
            if (maxBidField != null) maxBidField.setDisable(true);
            if (incrementField != null) incrementField.setDisable(true);
        });
    }

    private void onMessage(Message msg) {
        if (!isActive || auctionId == null) return;
        if (!msg.getPayload().contains(auctionId)) return;

        if (msg.getType() == MessageType.BID_UPDATE || msg.getType() == MessageType.SUCCESS) {
            handleBidUpdate(msg);
        }
    }

    private void handleBidUpdate(Message msg) {
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < UPDATE_THROTTLE_MS) return;
        lastUpdateTime = now;

        Platform.runLater(() -> {
            try {
                BidTransaction bid = JsonUtil.fromJson(msg.getPayload(), BidTransaction.class);
                if (bid == null) return;

                if (currentPriceLabel != null) {
                    currentPriceLabel.setText(String.format("%,.0f VNĐ", bid.getAmount()));
                }

                bidHistoryChart.addBidPoint(bid);
                updateBidTable(bid);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateBidTable(BidTransaction bid) {
        bidList.add(0, bid);
        if (bidList.size() > 25) {
            bidList.remove(bidList.size() - 1);
        }
    }

    @FXML
    public void handleBid() {
        if (isEnded || auctionId == null) return;
        try {
            double amount = Double.parseDouble(bidAmountField.getText().trim());
            if (amount > 0) {
                auctionService.placeBid(auctionId, currentUserId, amount);
                bidAmountField.clear();
            }
        } catch (Exception e) {
            showAlert("Lỗi", "Số tiền không hợp lệ.");
        }
    }

    @FXML
    public void handleAutoBid() {
        if (isEnded) return;
        try {
            double maxBid = Double.parseDouble(maxBidField.getText().trim());
            double increment = Double.parseDouble(incrementField.getText().trim());
            if (maxBid > 0 && increment > 0) {
                autoBidService.registerAutoBid(auctionId, currentUserId, maxBid, increment);
                showAlert("Thành công", "Đã đăng ký Auto Bid!");
            }
        } catch (Exception e) {
            showAlert("Lỗi", "Vui lòng nhập số hợp lệ.");
        }
    }

    @FXML
    public void goBack() {
        cleanup();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/AuctionDashboard.fxml"));
            Stage stage = (Stage) itemNameLabel.getScene().getWindow();
            Scene scene = new Scene(loader.load(), 1150, 720);
            stage.setScene(scene);
            stage.setTitle("Latest Auctions");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanup() {
        isActive = false;
        cleanupTimer();
        SocketClient.getInstance().removeListener(this::onMessage);
        if (bidHistoryChart != null) {
            bidHistoryChart.clear();
        }
    }

    private void cleanupTimer() {
        if (countdownExecutor != null) {
            countdownExecutor.shutdownNow();
            countdownExecutor = null;
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