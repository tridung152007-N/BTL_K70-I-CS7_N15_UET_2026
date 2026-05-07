package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import com.auction.app.AuctionApp;
import com.auction.service.AuctionService;
import java.util.List;

/**
 * Controller cho màn hình danh sách đấu giá
 * Hiển thị danh sách sản phẩm và xử lý navigation
 */
public class MainController {

    @FXML
    private GridPane auctionGrid;

    @FXML
    private ComboBox<String> categoryFilter;

    @FXML
    private ComboBox<String> sortFilter;

    @FXML
    private TextField searchField;

    private AuctionService auctionService = AuctionService.getInstance();

    @FXML
    public void initialize() {
        // Set default values
        categoryFilter.setValue("Tất cả danh mục");
        sortFilter.setValue("Mới nhất");

        // Load auction items
        loadAuctionItems();

        // Add listeners for filters
        categoryFilter.setOnAction(e -> loadAuctionItems());
        sortFilter.setOnAction(e -> loadAuctionItems());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadAuctionItems());
    }

    /**
     * Load auction items from service and display in grid
     */
    private void loadAuctionItems() {
        auctionGrid.getChildren().clear();

        // Get all auctions from service
        List<AuctionService.AuctionDTO> auctions = auctionService.getAllAuctions();

        // Display each auction as a card
        int col = 0, row = 0;
        for (AuctionService.AuctionDTO auction : auctions) {
            if (col >= 4) {
                col = 0;
                row++;
            }
            auctionGrid.add(createAuctionCard(auction), col, row);
            col++;
        }
    }

    /**
     * Create an auction card component
     */
    private VBox createAuctionCard(AuctionService.AuctionDTO auction) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPrefHeight(300);
        card.setCursor(javafx.scene.Cursor.HAND);

        // Add click handler to navigate to detail screen
        card.setOnMouseClicked(e -> {
            AuctionApp.showAuctionDetailScreen(auction.getId());
        });

        // Product image placeholder
        StackPane imagePlaceholder = new StackPane();
        imagePlaceholder.setStyle("-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -fx-border-radius: 8; -fx-background-radius: 8;");
        imagePlaceholder.setPrefHeight(150);
        Label imageLabel = new Label("🖼️");
        imageLabel.setStyle("-fx-font-size: 48;");
        imagePlaceholder.getChildren().add(imageLabel);

        // Product name
        Label productName = new Label(auction.getName());
        productName.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Category
        Label category = new Label(auction.getCategory());
        category.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");

        // Price info
        VBox priceBox = new VBox(3);
        Label currentPrice = new Label("₫ " + formatPrice(auction.getCurrentPrice()));
        currentPrice.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        Label startPrice = new Label("Khởi điểm: ₫ " + formatPrice(auction.getStartingPrice()));
        startPrice.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");
        priceBox.getChildren().addAll(currentPrice, startPrice);

        // Auction info
        HBox infoBox = new HBox(15);
        Label bids = new Label("📊 " + auction.getTotalBids() + " bids");
        bids.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");
        Label timeLeft = new Label("⏱️ 5h");
        timeLeft.setStyle("-fx-font-size: 11; -fx-text-fill: #e74c3c;");
        infoBox.getChildren().addAll(bids, timeLeft);

        card.getChildren().addAll(
                imagePlaceholder,
                productName,
                category,
                priceBox,
                new Separator(),
                infoBox
        );

        return card;
    }

    /**
     * Format price with thousand separators
     */
    private String formatPrice(long price) {
        return String.format("%,d", price).replace(",", ".");
    }
}