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

    // Theo dõi request đang chờ (APPROVE / REJECT) để phân biệt SUCCESS
    private MessageType pendingRequest = null;

    @FXML
    public void initialize() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colSeller.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSellerId()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            selectedItem = item;
            if (item != null) showDetail(item);
        });

        msgListener = this::onMessage;
        SocketClient.getInstance().addListener(msgListener);

        // Load lần đầu
        handleRefresh();

        // Tự remove listener khi rời màn hình
        itemTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) cleanup();
        });
    }

    public void cleanup() {
        SocketClient.getInstance().removeListener(msgListener);
    }

    @FXML
    public void handleRefresh() {
        String currentSelectedId = (selectedItem != null) ? selectedItem.getId() : null;
        adminService.getPendingItems();

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
                pendingRequest = MessageType.ITEM_APPROVE;
                adminService.approveItem(selectedItem.getId());
                setStatus("⏳ Đang duyệt...", true);
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
                pendingRequest = MessageType.ITEM_REJECT;
                adminService.rejectItem(selectedItem.getId());
                setStatus("⏳ Đang từ chối...", true);
            }
        });
    }

    private void onMessage(Message msg) {
        switch (msg.getType()) {
            case ITEM_PENDING_LIST -> {
                // Nhận cả khi tự request lẫn khi server broadcast sau ITEM_ADD
                List<Item> items = parseItemList(msg.getPayload());
                Platform.runLater(() -> {
                    itemTable.setItems(FXCollections.observableArrayList(items));
                    setStatus(items.isEmpty()
                            ? "Không có sản phẩm nào đang chờ duyệt."
                            : "Có " + items.size() + " sản phẩm chờ duyệt.", !items.isEmpty());
                    System.out.println("📋 Admin nhận ITEM_PENDING_LIST: " + items.size() + " items");
                });
            }
            case SUCCESS -> {
                // Chỉ xử lý khi màn hình này đang chờ phản hồi
                if (pendingRequest != null) {
                    MessageType req = pendingRequest;
                    pendingRequest = null;
                    Platform.runLater(() -> {
                        String successMsg = switch (req) {
                            case ITEM_APPROVE -> "✅ Đã duyệt sản phẩm thành công.";
                            case ITEM_REJECT  -> "✅ Đã từ chối sản phẩm.";
                            default           -> "✅ Thao tác thành công.";
                        };
                        setStatus(successMsg, true);
                        handleRefresh(); // Reload bảng sau khi approve/reject
                    });
                }
            }
            case ERROR -> {
                if (pendingRequest != null) {
                    pendingRequest = null;
                    try {
                        String err = JsonUtil.fromJson(msg.getPayload(), ErrPayload.class).error();
                        Platform.runLater(() -> setStatus("❌ Lỗi: " + err, false));
                    } catch (Exception e) {
                        Platform.runLater(() -> setStatus("❌ Lỗi không xác định", false));
                    }
                }
            }
            // Bỏ qua tất cả type khác (BID_UPDATE, AUCTION_LIST, ...)
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
