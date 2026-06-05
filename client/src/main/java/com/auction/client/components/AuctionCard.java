package com.auction.client.components;

import com.auction.server.model.Auction;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class AuctionCard extends VBox {

    private Label priceLabel;
    private Label leaderLabel;
    private Label timeLabel;
    private final Auction auction;

    public AuctionCard(Auction auction, Runnable onClick) {
        super();
        this.auction = auction;

        setPrefSize(240, 340);
        setStyle("""
            -fx-background-color: white;
            -fx-border-color: #e0e0e0;
            -fx-border-width: 1;
            -fx-border-radius: 8;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);
            """);

        // ==================== IMAGE SECTION ====================
        ImageView imageView = createProductImage(auction.getItemImagePath());
        VBox imageContainer = new VBox(imageView);
        imageContainer.setPrefHeight(200);
        imageContainer.setStyle("-fx-background-color: #f5f5f5;");
        imageContainer.setAlignment(Pos.CENTER);
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(180);
        imageView.setFitWidth(180);
        VBox.setVgrow(imageContainer, Priority.ALWAYS);

        // ==================== CONTENT SECTION ====================
        VBox contentBox = new VBox(8);
        contentBox.setPadding(new Insets(12));

        // Tên sản phẩm
        Label nameLabel = new Label(auction.getItemName() != null && !auction.getItemName().isEmpty()
                ? auction.getItemName() : "Sản phẩm #" + auction.getItemId());
        nameLabel.setFont(Font.font(14));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");
        nameLabel.setWrapText(true);

        // Giá hiện tại - Lưu reference để cập nhật realtime
        priceLabel = new Label(String.format("%,.0f VNĐ", auction.getCurrentPrice()));
        priceLabel.setFont(Font.font(16));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        leaderLabel = new Label(formatLeader());
        leaderLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #34495e;");
        leaderLabel.setWrapText(true);

        // ==================== THỜI GIAN ĐẾM NGƯỢC ====================
        timeLabel = new Label();
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        updateTimeDisplay();

        HBox infoBox = new HBox(15);
        infoBox.setStyle("-fx-text-fill: #95a5a6;");
        infoBox.getChildren().add(timeLabel);

        // Action buttons
        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(0, 0, 8, 0));

        Label watchBtn = createActionButton("👁");
        Label favoriteBtn = createActionButton("♡");
        actionBox.getChildren().addAll(watchBtn, favoriteBtn);

        contentBox.getChildren().addAll(nameLabel, priceLabel, leaderLabel, infoBox, actionBox);

        // ==================== ASSEMBLE ====================
        getChildren().addAll(imageContainer, contentBox);

        // Click event
        setOnMouseClicked(e -> onClick.run());

        // Hover effect
        setOnMouseEntered(e -> setStyle(getStyle() + "-fx-border-color: #3498db; -fx-border-width: 2;"));
        setOnMouseExited(e -> setStyle(getStyle().replace("-fx-border-color: #3498db; -fx-border-width: 2;", "-fx-border-color: #e0e0e0; -fx-border-width: 1;")));
    }

    /**
     * Cập nhật giá realtime khi có bid mới
     */
    public void updatePrice(double newPrice) {
        Platform.runLater(() -> {
            if (priceLabel != null) {
                auction.setCurrentPrice(newPrice);
                priceLabel.setText(String.format("%,.0f VNĐ", newPrice));
            }
        });
    }

    private String formatLeader() {
        String leader = auction.getCurrentLeaderName();
        if (leader == null || leader.isBlank()) {
            leader = auction.getCurrentLeaderId();
        }
        return leader == null || leader.isBlank() ? "Chua co nguoi dan dau" : "Dang dan dau: " + leader;
    }

    /**
     * Cập nhật hiển thị thời gian đếm ngược hoặc trạng thái kết thúc
     */
    private void updateTimeDisplay() {
        if (auction.getEndTime() == null) {
            timeLabel.setText("⏱ Không xác định");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long secondsLeft = ChronoUnit.SECONDS.between(now, auction.getEndTime());

        if (secondsLeft <= 0) {
            timeLabel.setText("✅ Phiên đã kết thúc");
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else {
            long hours = secondsLeft / 3600;
            long minutes = (secondsLeft % 3600) / 60;
            String timeStr = String.format("⏱ %dh %02dm left", hours, minutes);
            timeLabel.setText(timeStr);
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        }
    }

    private ImageView createProductImage(String imagePath) {
        ImageView imageView = new ImageView();
        imageView.setFitHeight(160);
        imageView.setFitWidth(160);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(false);   // Tắt smooth để nhanh hơn

        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    // Load với cache + background
                    Image image = new Image(file.toURI().toString(), 160, 160, true, false);
                    imageView.setImage(image);
                } else {
                    imageView.setImage(getDefaultImage());
                }
            } catch (Exception e) {
                imageView.setImage(getDefaultImage());
            }
        } else {
            imageView.setImage(getDefaultImage());
        }
        return imageView;
    }

    private Image getDefaultImage() {
        String svg = """
            <svg width="180" height="180" xmlns="http://www.w3.org/2000/svg">
              <rect width="180" height="180" fill="#ecf0f1"/>
              <text x="50%" y="50%" text-anchor="middle" dy=".3em" font-size="48" fill="#bdc3c7">📦</text>
            </svg>
            """;
        try {
            return new Image("data:image/svg+xml," + svg.replace("\n", ""));
        } catch (Exception e) {
            return new ImageView().getImage();
        }
    }

    private Label createActionButton(String icon) {
        Label btn = new Label(icon);
        btn.setPrefSize(40, 40);
        btn.setStyle("""
            -fx-alignment: CENTER;
            -fx-background-color: #1abc9c;
            -fx-text-fill: white;
            -fx-font-size: 18px;
            -fx-background-radius: 50;
            -fx-cursor: hand;
            """);
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace("#1abc9c", "#16a085")));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("#16a085", "#1abc9c")));
        return btn;
    }
}
