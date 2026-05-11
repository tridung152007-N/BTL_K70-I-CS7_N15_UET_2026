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
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class BidRoomController {
    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label timerLabel;
    @FXML private TextField bidAmountField;
    @FXML private TextField maxBidField;
    @FXML private TextField incrementField;
    @FXML private StackPane chartContainer;

    private final AuctionClientService auctionService = new AuctionClientService();
    private final AutoBidClientService autoBidService = new AutoBidClientService();
    private final BidHistoryChart bidHistoryChart = new BidHistoryChart();
    private String auctionId;
    private String currentUserId;

    @FXML
    public void initialize() {
        chartContainer.getChildren().add(bidHistoryChart.getChart());
        SocketClient.getInstance().addListener(this::onMessage);
    }

    @FXML
    public void handleBid() {
        try {
            double amount = Double.parseDouble(bidAmountField.getText().trim());
            auctionService.placeBid(auctionId, currentUserId, amount);
            bidAmountField.clear();
        } catch (NumberFormatException e) {
            // TODO: show error alert
        }
    }

    @FXML
    public void handleAutoBid() {
        try {
            double maxBid    = Double.parseDouble(maxBidField.getText().trim());
            double increment = Double.parseDouble(incrementField.getText().trim());
            autoBidService.registerAutoBid(auctionId, currentUserId, maxBid, increment);
        } catch (NumberFormatException e) {
            // TODO: show error alert
        }
    }

    private void onMessage(Message msg) {
        if (msg.getType() == MessageType.BID_UPDATE) {
            BidTransaction bid = JsonUtil.fromJson(msg.getPayload(), BidTransaction.class);
            Platform.runLater(() -> {
                currentPriceLabel.setText(String.format("%,.0f VNĐ", bid.getAmount()));
                bidHistoryChart.addBidPoint(bid);
            });
        } else if (msg.getType() == MessageType.AUCTION_END) {
            Platform.runLater(() -> timerLabel.setText("Phiên đã kết thúc!"));
        }
    }

    public void setAuction(String auctionId, String itemName) {
        this.auctionId = auctionId;
        itemNameLabel.setText(itemName);
    }

    public void setCurrentUserId(String userId) { this.currentUserId = userId; }
}
