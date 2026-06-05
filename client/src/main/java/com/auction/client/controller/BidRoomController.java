package com.auction.client.controller;

import com.auction.client.chart.BidHistoryChart;
import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.AutoBidClientService;
import com.auction.client.session.UserSession;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.BidTransaction;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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
    @FXML private Label currentLeaderLabel;
    @FXML private Label walletBalanceLabel;
    @FXML private Label paymentStatusLabel;
    @FXML private Label timerLabel;
    @FXML private TextField bidAmountField;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private StackPane chartContainer;
    @FXML private TableView<BidTransaction> bidHistoryTable;
    @FXML private TableColumn<BidTransaction, String> colBidder;
    @FXML private TableColumn<BidTransaction, Double> colAmount;
    @FXML private TableColumn<BidTransaction, Long> colTime;
    @FXML private Button payButton;

    private final ObservableList<BidTransaction> bidList = FXCollections.observableArrayList();
    private final AuctionClientService auctionService = new AuctionClientService();
    private final AutoBidClientService autoBidService = new AutoBidClientService();
    private final BidHistoryChart bidHistoryChart = new BidHistoryChart();

    private String auctionId;
    private String currentUserId;
    private String itemName;
    private String currentLeaderId;
    private LocalDateTime endTime;
    private ScheduledExecutorService countdownExecutor;
    private volatile boolean isActive = true;
    private volatile boolean isEnded = false;
    private boolean paid;

    @FXML
    public void initialize() {
        setupChart();
        setupTable();
        SocketClient.getInstance().addListener(this::onMessage);
        updateWalletLabel(UserSession.getInstance().getBalance());
        updatePaymentUi();
    }

    private void setupChart() {
        if (chartContainer != null) {
            chartContainer.getChildren().clear();
            chartContainer.getChildren().add(bidHistoryChart.getChart());
        }
    }

    private void setupTable() {
        if (bidHistoryTable != null) {
            colBidder.setCellValueFactory(new PropertyValueFactory<>("bidderName"));
            colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
            colTime.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
            bidHistoryTable.setItems(bidList);
        }
    }

    public void setAuction(String auctionId, String itemName, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.itemName = itemName != null && !itemName.isEmpty() ? itemName : "Phien #" + auctionId;
        this.endTime = endTime;
        if (itemNameLabel != null) itemNameLabel.setText(this.itemName);
        if (endTime != null) startCountdown();
        auctionService.requestBidHistory(auctionId);
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        auctionService.requestWalletBalance(userId);
        updatePaymentUi();
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
            timerLabel.setText(String.format("%02d:%02d:%02d con lai", hours, minutes, seconds));
        });
    }

    private void auctionEnded() {
        isEnded = true;
        cleanupTimer();
        Platform.runLater(() -> {
            timerLabel.setText("Phien da ket thuc");
            if (bidAmountField != null) bidAmountField.setDisable(true);
            if (maxBidField != null) maxBidField.setDisable(true);
            if (incrementField != null) incrementField.setDisable(true);
            updatePaymentUi();
        });
    }

    private void onMessage(Message msg) {
        if (!isActive) return;

        switch (msg.getType()) {
            case BID_HISTORY -> handleBidHistory(msg);
            case BID_UPDATE -> {
                if (auctionId != null && msg.getPayload().contains(auctionId)) handleBidUpdate(msg);
            }
            case WALLET_BALANCE -> handleWalletUpdate(msg);
            case PAYMENT_PAY -> handlePaymentResult(msg);
            case ERROR -> handleError(msg);
            default -> {
            }
        }
    }

    private void handleBidHistory(Message msg) {
        if (auctionId == null) return;
        try {
            JsonObject obj = JsonParser.parseString(msg.getPayload()).getAsJsonObject();
            if (!auctionId.equals(obj.get("auctionId").getAsString())) return;
            BidTransaction[] history = JsonUtil.fromJson(obj.get("bids").toString(), BidTransaction[].class);
            Platform.runLater(() -> {
                bidList.clear();
                bidHistoryChart.clear();
                currentLeaderId = readString(obj, "currentLeaderId");
                paid = obj.has("paid") && obj.get("paid").getAsBoolean();
                for (BidTransaction bid : history) {
                    bidHistoryChart.addBidPoint(bid);
                    bidList.add(0, bid);
                }
                if (obj.has("currentPrice") && currentPriceLabel != null) {
                    currentPriceLabel.setText(String.format("%,.0f VND", obj.get("currentPrice").getAsDouble()));
                }
                updateLeaderLabel(readString(obj, "currentLeaderName"));
                updatePaymentUi();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleBidUpdate(Message msg) {
        Platform.runLater(() -> {
            try {
                BidTransaction bid = JsonUtil.fromJson(msg.getPayload(), BidTransaction.class);
                if (bid == null) return;
                currentLeaderId = bid.getBidderId();
                paid = false;
                if (currentPriceLabel != null) currentPriceLabel.setText(String.format("%,.0f VND", bid.getAmount()));
                updateLeaderLabel(bid.getBidderName());
                bidHistoryChart.addBidPoint(bid);
                bidList.add(0, bid);
                if (bidList.size() > 50) bidList.remove(bidList.size() - 1);
                updatePaymentUi();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleWalletUpdate(Message msg) {
        try {
            JsonObject obj = JsonParser.parseString(msg.getPayload()).getAsJsonObject();
            if (currentUserId == null || !currentUserId.equals(obj.get("userId").getAsString())) return;
            double balance = obj.get("balance").getAsDouble();
            UserSession.getInstance().setBalance(balance);
            Platform.runLater(() -> updateWalletLabel(balance));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePaymentResult(Message msg) {
        try {
            JsonObject obj = JsonParser.parseString(msg.getPayload()).getAsJsonObject();
            if (auctionId == null || !auctionId.equals(obj.get("auctionId").getAsString())) return;
            paid = obj.get("paid").getAsBoolean();
            double balance = obj.get("balance").getAsDouble();
            UserSession.getInstance().setBalance(balance);
            Platform.runLater(() -> {
                updateWalletLabel(balance);
                updatePaymentUi();
                showAlert("Thanh toan", "Thanh toan thanh cong.");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleError(Message msg) {
        try {
            JsonObject obj = JsonParser.parseString(msg.getPayload()).getAsJsonObject();
            Platform.runLater(() -> showAlert("Loi", obj.get("error").getAsString()));
        } catch (Exception ignored) {
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
            showAlert("Loi", "So tien khong hop le.");
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
                showAlert("Thanh cong", "Da dang ky Auto Bid.");
            }
        } catch (Exception e) {
            showAlert("Loi", "Vui long nhap so hop le.");
        }
    }

    @FXML
    public void handlePay() {
        if (auctionId == null || currentUserId == null) return;
        auctionService.payAuction(auctionId, currentUserId);
    }

    @FXML
    public void goBack() {
        cleanup();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/AuctionDashboard.fxml"));
            Stage stage = (Stage) itemNameLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1150, 720));
            stage.setTitle("Latest Auctions");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateWalletLabel(double balance) {
        if (walletBalanceLabel != null) walletBalanceLabel.setText(String.format("Vi: %,.0f VND", balance));
    }

    private void updateLeaderLabel(String leaderName) {
        if (currentLeaderLabel == null) return;
        String value = leaderName;
        if (value == null || value.isBlank()) value = currentLeaderId;
        currentLeaderLabel.setText(value == null || value.isBlank()
                ? "Dang dan dau: chua co"
                : "Dang dan dau: " + value);
    }

    private void updatePaymentUi() {
        boolean isWinner = currentUserId != null && currentUserId.equals(currentLeaderId);
        boolean canPay = isEnded && isWinner && !paid;
        if (payButton != null) payButton.setDisable(!canPay);
        if (paymentStatusLabel != null) {
            if (paid) paymentStatusLabel.setText("Da thanh toan");
            else if (isWinner) paymentStatusLabel.setText(isEnded ? "Ban da thang, co the thanh toan" : "Ban dang dan dau");
            else paymentStatusLabel.setText("Chua den luot thanh toan");
        }
    }

    private String readString(JsonObject obj, String name) {
        return obj.has(name) && !obj.get(name).isJsonNull() ? obj.get(name).getAsString() : null;
    }

    private void cleanup() {
        isActive = false;
        cleanupTimer();
        SocketClient.getInstance().removeListener(this::onMessage);
        bidHistoryChart.clear();
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
