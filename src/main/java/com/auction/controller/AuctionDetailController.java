package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.application.Platform;
import com.auction.app.AuctionApp;

/**
 * Controller cho chi tiết phiên đấu giá với realtime bidding
 */
public class AuctionDetailController {

    @FXML
    private ImageView productImage;

    @FXML
    private Label productName;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label auctionStatus;

    @FXML
    private Label endTimeLabel;

    @FXML
    private Label startPriceLabel;

    @FXML
    private Label currentPriceLabel;

    @FXML
    private Label highestBidderLabel;

    @FXML
    private Label bidCountLabel;

    @FXML
    private Label minBidLabel;

    @FXML
    private TextField bidAmountField;

    @FXML
    private Button placeBidButton;

    @FXML
    private CheckBox autobidCheckbox;

    @FXML
    private Label messageLabel;

    @FXML
    private TableView<BidHistory> bidHistoryTable;

    @FXML
    private Label descriptionLabel;

    private int remainingSeconds = 305; // 5 minutes 5 seconds
    private long currentPrice = 3500000;
    private long minBid = 3600000;

    @FXML
    public void initialize() {
        // Set default values
        productName.setText("iPhone 15 Pro Max - Chính hãng");
        categoryLabel.setText("Danh mục: Điện tử");
        startPriceLabel.setText("₫ 1,000,000");
        currentPriceLabel.setText("₫ 3,500,000");
        highestBidderLabel.setText("Bởi: Nguyễn Văn A");
        bidCountLabel.setText("45");
        minBidLabel.setText("₫ 3,600,000");
        descriptionLabel.setText(
                "iPhone 15 Pro Max - Điện thoại cao cấp nhất từ Apple với chip A17 Pro, camera 48MP, " +
                        "màn hình Super Retina XDR 120Hz. Tình trạng: như mới, có đầy đủ phụ kiện, bảo hành còn 24 tháng."
        );

        // Start countdown timer
        startCountdownTimer();

        // Add listener for bid input validation
        bidAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                long bidAmount = Long.parseLong(newVal);
                if (bidAmount < minBid) {
                    bidAmountField.setStyle(bidAmountField.getStyle() + "; -fx-border-color: #e74c3c;");
                } else {
                    bidAmountField.setStyle(bidAmountField.getStyle() + "; -fx-border-color: #27ae60;");
                }
            } catch (NumberFormatException e) {
                // Invalid number
            }
        });

        // Load bid history
        loadBidHistory();
    }

    /**
     * Handle back button click
     */
    @FXML
    private void handleBackClick() {
        AuctionApp.showMainScreen();
    }

    /**
     * Handle place bid action
     */
    @FXML
    private void handlePlaceBid() {
        try {
            long bidAmount = Long.parseLong(bidAmountField.getText());

            // Validate bid
            if (bidAmount < minBid) {
                messageLabel.setText("❌ Giá đặt phải tối thiểu ₫ " + minBid);
                messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                return;
            }

            // TODO: Send bid to server
            // For demo, we'll update UI locally
            currentPrice = bidAmount;
            minBid = bidAmount + 100000;
            currentPriceLabel.setText("₫ " + formatPrice(bidAmount));
            minBidLabel.setText("₫ " + formatPrice(minBid));
            highestBidderLabel.setText("Bởi: Bạn");

            // Update bid count
            int bidCount = Integer.parseInt(bidCountLabel.getText());
            bidCountLabel.setText(String.valueOf(bidCount + 1));

            // Clear input
            bidAmountField.clear();

            // Show success message
            messageLabel.setText("✅ Đặt giá thành công!");
            messageLabel.setStyle("-fx-text-fill: #27ae60;");

            // Add to history
            addBidToHistory("Tôi", bidAmount);

        } catch (NumberFormatException e) {
            messageLabel.setText("❌ Vui lòng nhập một số hợp lệ!");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    /**
     * Start countdown timer for auction end time
     */
    private void startCountdownTimer() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    remainingSeconds--;

                    // Format time
                    int hours = remainingSeconds / 3600;
                    int minutes = (remainingSeconds % 3600) / 60;
                    int seconds = remainingSeconds % 60;

                    String timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    endTimeLabel.setText(timeText);

                    // Change color if time is running out
                    if (remainingSeconds < 300) {
                        endTimeLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }

                    // Auto-bid if enabled (simple logic)
                    if (autobidCheckbox.isSelected() && remainingSeconds > 0 && remainingSeconds < 5) {
                        // TODO: Implement auto-bid logic
                    }

                    if (remainingSeconds <= 0) {
                        auctionStatus.setText("🔴 Đã kết thúc");
                        auctionStatus.setStyle("-fx-text-fill: #e74c3c;");
                        placeBidButton.setDisable(true);
                        bidAmountField.setDisable(true);
                    }
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Load bid history from backend
     */
    private void loadBidHistory() {
        // TODO: Load actual bid history from server
        // For demo, we'll add some sample data
        addBidToHistory("Nguyễn Văn A", 3500000);
        addBidToHistory("Trần Thị B", 3400000);
        addBidToHistory("Lê Văn C", 3300000);
        addBidToHistory("Phạm Quốc D", 3200000);
    }

    /**
     * Add bid to history table
     */
    private void addBidToHistory(String bidder, long amount) {
        Platform.runLater(() -> {
            // TODO: Add to bidHistoryTable
            System.out.println("Bid added: " + bidder + " - ₫ " + amount);
        });
    }

    /**
     * Format price with thousand separators
     */
    private String formatPrice(long price) {
        return String.format("%,d", price).replace(",", ".");
    }

    /**
     * Simple BidHistory model (for TableView)
     */
    public static class BidHistory {
        private String bidder;
        private long amount;
        private String timestamp;
        private String status;

        public BidHistory(String bidder, long amount, String timestamp, String status) {
            this.bidder = bidder;
            this.amount = amount;
            this.timestamp = timestamp;
            this.status = status;
        }

        public String getBidder() { return bidder; }
        public long getAmount() { return amount; }
        public String getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
    }
}