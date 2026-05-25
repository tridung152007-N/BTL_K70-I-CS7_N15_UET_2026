package com.auction.client.controller;

import com.auction.client.chart.BidHistoryChart;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.AutoBidClientService;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.BidTransaction;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
    private final BidHistoryChart bidHistoryChart = new BidHistoryChart();

    private String auctionId;
    private String currentUserId;
    private String itemName;
    private LocalDateTime endTime;
    private Timeline countdownTimer;
    private boolean isEnded = false;

    @FXML
    public void initialize() {
        System.out.println("🔧 BidRoomController initialize() called");

        if (chartContainer != null) {
            chartContainer.getChildren().clear();
            chartContainer.getChildren().add(bidHistoryChart.getChart());
        }

        // Setup bảng lịch sử
        if (bidHistoryTable != null) {
            colBidder.setCellValueFactory(new PropertyValueFactory<>("bidderId"));
            colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
            colTime.setCellValueFactory(new PropertyValueFactory<>("createdAt")); // có thể format sau
            bidHistoryTable.setItems(bidList);
        }

        SocketClient.getInstance().addListener(this::onMessage);
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
        System.out.println("✅ setAuction() hoàn tất cho auction: " + auctionId);
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        System.out.println("👤 Current User ID: " + userId);
    }

    private void startCountdown() {
        if (countdownTimer != null) countdownTimer.stop();

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateCountdown() {
        if (endTime == null || isEnded) return;

        LocalDateTime now = LocalDateTime.now();
        long secondsLeft = ChronoUnit.SECONDS.between(now, endTime);

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
    }

    private void auctionEnded() {
        isEnded = true;
        if (countdownTimer != null) countdownTimer.stop();

        timerLabel.setText("✅ Phiên đã kết thúc!");
        timerLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        if (bidAmountField != null) bidAmountField.setDisable(true);
        if (maxBidField != null) maxBidField.setDisable(true);
        if (incrementField != null) incrementField.setDisable(true);

        showAlert("Thông báo", "Phiên đấu giá đã kết thúc!");
    }

    @FXML
    public void handleBid() {
        if (isEnded) return;
        try {
            double amount = Double.parseDouble(bidAmountField.getText().trim());
            if (amount <= 0) {
                showAlert("Lỗi", "Số tiền phải lớn hơn 0.");
                return;
            }
            auctionService.placeBid(auctionId, currentUserId, amount);
            bidAmountField.clear();
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

            if (maxBid <= 0 || increment <= 0) {
                showAlert("Lỗi", "Giá tối đa và bước giá phải lớn hơn 0.");
                return;
            }

            autoBidService.registerAutoBid(auctionId, currentUserId, maxBid, increment);
            showAlert("Thành công", "Đã đăng ký Auto Bid thành công!");
        } catch (Exception e) {
            showAlert("Lỗi", "Vui lòng nhập số hợp lệ.");
        }
    }

    private void onMessage(Message msg) {
        Platform.runLater(() -> {
            if (msg.getType() == MessageType.BID_UPDATE || msg.getType() == MessageType.SUCCESS) {
                try {
                    BidTransaction bid = JsonUtil.fromJson(msg.getPayload(), BidTransaction.class);
                    if (bid != null) {
                        // Cập nhật giá
                        if (currentPriceLabel != null) {
                            currentPriceLabel.setText(String.format("%,.0f VNĐ", bid.getAmount()));
                        }

                        // Thêm vào biểu đồ
                        bidHistoryChart.addBidPoint(bid);

                        // Thêm vào bảng
                        bidList.add(0, bid); // thêm lên đầu
                        if (bidList.size() > 20) bidList.remove(bidList.size() - 1);

                        // Thông báo
                        System.out.println("🔔 " + bid.getBidderId() + " vừa bid " + String.format("%,.0f VNĐ", bid.getAmount()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void goBack() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        bidHistoryChart.clear();

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

    private record ErrorPayload(String error) {}
}