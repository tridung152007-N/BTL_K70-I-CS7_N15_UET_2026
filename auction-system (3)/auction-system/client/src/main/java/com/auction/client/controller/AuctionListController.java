package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionClientService;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.Auction;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class AuctionListController {

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> nameCol;
    @FXML private TableColumn<Auction, String> priceCol;
    @FXML private TableColumn<Auction, String> endCol;
    @FXML private TableColumn<Auction, String> stateCol;

    private final AuctionClientService service = new AuctionClientService();
    private String currentUserId = "bidder1"; // TODO: inject từ login

    @FXML
    public void initialize() {
        // Gán dữ liệu cho từng cột
        nameCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItemId())); // TODO: hiện tên item thay vì id
        priceCol.setCellValueFactory(c ->
                new SimpleStringProperty(String.format("%,.0f VNĐ", c.getValue().getCurrentPrice())));
        endCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEndTime() != null
                        ? c.getValue().getEndTime().toString() : ""));
        stateCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getState().name()));

        // Nhấp đúp → vào phòng đấu giá
        auctionTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && auctionTable.getSelectionModel().getSelectedItem() != null) {
                goToBidRoom(auctionTable.getSelectionModel().getSelectedItem());
            }
        });

        // Lắng nghe server
        SocketClient.getInstance().addListener(this::onMessage);
        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        service.requestAuctionList(currentUserId);
    }

    private void onMessage(Message msg) {
        if (msg.getType() == MessageType.AUCTION_LIST) {
            List<Auction> list = Arrays.asList(
                    JsonUtil.fromJson(msg.getPayload(), Auction[].class));
            Platform.runLater(() ->
                    auctionTable.setItems(FXCollections.observableArrayList(list)));
        }
    }

    private void goToBidRoom(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/BidRoom.fxml"));
            Stage stage = (Stage) auctionTable.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 900, 650));
            BidRoomController ctrl = loader.getController();
            ctrl.setAuction(auction.getId(), auction.getItemId());
            ctrl.setCurrentUserId(currentUserId);
            stage.setTitle("Phòng đấu giá – " + auction.getItemId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
