package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AdminClientService;
import com.auction.client.session.UserSession;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AdminController {

    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, String> colCategory;
    @FXML private TableColumn<Item, String> colSeller;
    @FXML private TableColumn<Item, String> colStatus;
    @FXML private Label statusLabel;
    @FXML private TextArea detailArea;

    private final AdminClientService adminService = new AdminClientService();
    private Item selectedItem = null;
    private com.auction.client.network.MessageListener msgListener;
    private ScheduledExecutorService autoRefresh;

    @FXML
    public void initialize() {
        // Setup các cột bảng
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colSeller.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSellerId()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        // Click vào dòng để xem chi tiết
        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            selectedItem = item;
            if (item != null) showDetail(item);
        });

        msgListener = this::onMessage;
        SocketClient.getInstance().addListener(msgListener);

        // Chỉ refresh lần đầu khi mở màn hình
        handleRefresh();

        // ── TẮT AUTO REFRESH ─────────────────────────────
        // autoRefresh = Executors.newSingleThreadScheduledExecutor();
        // autoRefresh.scheduleAtFixedRate(this::handleRefresh, 8, 8, TimeUnit.SECONDS);

        // Gỡ listener khi rời màn hình
        itemTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                SocketClient.getInstance().removeListener(msgListener);
                if (autoRefresh != null) autoRefresh.shutdownNow();
            }
        });
    }

    @FXML
    public void handleRefresh() {
        String currentSelectedId = (selectedItem != null) ? selectedItem.getId() : null;

        adminService.getPendingItems();

        // Sau khi refresh xong, khôi phục selection
        Platform.runLater(() -> {
            if (currentSelectedId != null) {
                itemTable.getItems().stream()
                        .filter(item -> item.getId().equals(currentSelectedId))
                        .findFirst()
                        .ifPresent(item -> itemTable.getSelectionModel().select(item));
            }
        });
    }

    @FXML
    public void handleApprove() {
        if (selectedItem == null) {
            setStatus("⚠ Hãy chọn sản phẩm cần duyệt.", false);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Duyệt sản phẩm \"" + selectedItem.getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                adminService.approveItem(selectedItem.getId());
            }
        });
    }

    @FXML
    public void handleReject() {
        if (selectedItem == null) {
            setStatus("⚠ Hãy chọn sản phẩm cần từ chối.", false);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Từ chối sản phẩm \"" + selectedItem.getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                adminService.rejectItem(selectedItem.getId());
            }
        });
    }

    private void onMessage(Message msg) {
        System.out.println("🟢 Admin nhận message: " + msg.getType());  // Debug

        switch (msg.getType()) {
            case ITEM_PENDING_LIST -> {
                System.out.println("📋 Nhận danh sách pending từ server");
                List<Item> items = parseItemList(msg.getPayload());
                Platform.runLater(() -> {
                    itemTable.setItems(FXCollections.observableArrayList(items));
                    System.out.println("📊 Cập nhật bảng: " + items.size() + " items");
                    setStatus(items.isEmpty()
                            ? "Không có sản phẩm nào đang chờ duyệt."
                            : "Có " + items.size() + " sản phẩm chờ duyệt.", !items.isEmpty());
                });
            }
            case SUCCESS -> {
                System.out.println("✅ Nhận SUCCESS từ server");
                Platform.runLater(() -> {
                    setStatus("✅ Thao tác thành công.", true);
                    handleRefresh();
                });
            }
            case ERROR -> {
                System.out.println("❌ Nhận ERROR từ server");
                try {
                    String err = JsonUtil.fromJson(msg.getPayload(), ErrPayload.class).error();
                    Platform.runLater(() -> setStatus("❌ Lỗi: " + err, false));
                } catch (Exception e) {
                    Platform.runLater(() -> setStatus("❌ Lỗi không xác định", false));
                }
            }
        }
    }

    private void showDetail(Item item) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tên: ").append(item.getName()).append("\n");
        sb.append("Mô tả: ").append(item.getDescription()).append("\n");
        sb.append("Loại: ").append(item.getCategory()).append("\n");
        sb.append("Seller ID: ").append(item.getSellerId()).append("\n");
        sb.append("Trạng thái: ").append(item.getStatus()).append("\n");
        detailArea.setText(sb.toString());
    }

    private void setStatus(String msg, boolean ok) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (ok ? "#27ae60" : "#e67e22") + ";");
    }

    private List<Item> parseItemList(String payload) {
        List<Item> result = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(payload).getAsJsonArray();
            for (JsonElement el : arr) {
                String cat = "";
                try { cat = el.getAsJsonObject().get("category").getAsString(); } catch (Exception ignored) {}
                Class<? extends Item> clazz = switch (cat) {
                    case "ELECTRONICS" -> Electronics.class;
                    case "ART"         -> Art.class;
                    default            -> Vehicle.class;
                };
                result.add(JsonUtil.fromJson(el.toString(), clazz));
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse item list: " + e.getMessage());
        }
        return result;
    }

    private record ErrPayload(String error) {}
}
