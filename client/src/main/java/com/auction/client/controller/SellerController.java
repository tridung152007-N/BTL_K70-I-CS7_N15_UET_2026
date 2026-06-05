package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ItemClientService;
import com.auction.client.session.UserSession;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.stage.FileChooser;

import java.util.ArrayList;

public class SellerController {

    // ── Bảng danh sách sản phẩm ───────────────────────────
    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, String> colCategory;
    @FXML private TableColumn<Item, String> colStatus;

    // ── Form thêm/sửa sản phẩm ────────────────────────────
    @FXML private TextField nameField;
    @FXML private TextArea  descField;
    @FXML private ComboBox<String> categoryBox;
    @FXML private TextField brandField;
    @FXML private TextField warrantyField;
    @FXML private TextField artistField;
    @FXML private TextField artYearField;
    @FXML private TextField kmField;
    @FXML private TextField mfYearField;
    @FXML private VBox electronicsFields;
    @FXML private VBox artFields;
    @FXML private VBox vehicleFields;

    // ── Form tạo phiên đấu giá ────────────────────────────
    @FXML private TextField startingPriceField;
    @FXML private TextField durationHoursField;
    @FXML private Label statusLabel;
    @FXML private Label imagePathLabel;

    private final ItemClientService itemService       = new ItemClientService();
    private final AuctionClientService auctionService = new AuctionClientService();
    private String currentSellerId;
    private Item selectedItem = null;
    private com.auction.client.network.MessageListener msgListener;
    private String selectedImagePath = null;

    // Theo dõi loại request đang chờ phản hồi để phân biệt SUCCESS
    private MessageType pendingRequest = null;

    @FXML
    public void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                String extension = file.getName().substring(file.getName().lastIndexOf("."));
                String newFileName = UUID.randomUUID() + extension;

                File destFolder = new File("uploads");
                if (!destFolder.exists()) destFolder.mkdirs();

                File destFile = new File(destFolder, newFileName);
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                selectedImagePath = "uploads/" + newFileName;
                imagePathLabel.setText(newFileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void initialize() {
        currentSellerId = UserSession.getInstance().getUserId();

        // Cấu hình cột
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                switch (c.getValue().getStatus() == null ? "PENDING" : c.getValue().getStatus()) {
                    case "APPROVED" -> "✅ Đã duyệt";
                    case "REJECTED" -> "❌ Từ chối";
                    default         -> "⏳ Chờ duyệt";
                }));

        // Thiết lập ComboBox
        categoryBox.setItems(FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));
        categoryBox.setValue("ELECTRONICS");
        categoryBox.setOnAction(e -> showCategoryFields(categoryBox.getValue()));
        showCategoryFields("ELECTRONICS");

        // Listener chọn dòng trong bảng
        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedItem = newVal;
            if (newVal != null) fillForm(newVal);
        });

        // Đăng ký listener message từ server
        msgListener = this::onMessage;
        SocketClient.getInstance().addListener(msgListener);

        // Load dữ liệu lần đầu
        handleRefresh();

        // Không dùng auto-refresh polling nữa — dựa vào WebSocket event
    }

    public void cleanup() {
        SocketClient.getInstance().removeListener(msgListener);
    }

    private void showCategoryFields(String category) {
        electronicsFields.setVisible(false); electronicsFields.setManaged(false);
        artFields.setVisible(false);         artFields.setManaged(false);
        vehicleFields.setVisible(false);     vehicleFields.setManaged(false);
        switch (category) {
            case "ELECTRONICS" -> { electronicsFields.setVisible(true); electronicsFields.setManaged(true); }
            case "ART"         -> { artFields.setVisible(true);         artFields.setManaged(true); }
            case "VEHICLE"     -> { vehicleFields.setVisible(true);     vehicleFields.setManaged(true); }
        }
    }

    private void fillForm(Item item) {
        nameField.setText(item.getName() != null ? item.getName() : "");
        descField.setText(item.getDescription() != null ? item.getDescription() : "");
        categoryBox.setValue(item.getCategory());
        showCategoryFields(item.getCategory());
        if (item instanceof Electronics e) {
            brandField.setText(e.getBrand() != null ? e.getBrand() : "");
            warrantyField.setText(e.getWarrantyMonths() != null ? e.getWarrantyMonths() : "");
        } else if (item instanceof Art a) {
            artistField.setText(a.getArtist() != null ? a.getArtist() : "");
            artYearField.setText(a.getYear() > 0 ? String.valueOf(a.getYear()) : "");
        } else if (item instanceof Vehicle v) {
            kmField.setText(v.getKilometre() > 0 ? String.valueOf(v.getKilometre()) : "");
            mfYearField.setText(v.getManufacturingYear() > 0 ? String.valueOf(v.getManufacturingYear()) : "");
        }
    }

    @FXML
    public void handleAdd() {
        if (nameField.getText().trim().isEmpty()) {
            setStatus("⚠ Vui lòng nhập tên sản phẩm.", false);
            return;
        }
        Item item = buildItemFromForm();
        item.setId(UUID.randomUUID().toString());
        item.setSellerId(currentSellerId);
        item.setStatus("PENDING");

        pendingRequest = MessageType.ITEM_ADD;
        itemService.addItem(item);
        setStatus("⏳ Đang gửi...", true);
        clearForm();
    }

    @FXML
    public void handleEdit() {
        if (selectedItem == null) {
            setStatus("⚠ Hãy chọn sản phẩm cần sửa.", false);
            return;
        }
        Item updated = buildItemFromForm();
        updated.setId(selectedItem.getId());
        updated.setSellerId(currentSellerId);

        pendingRequest = MessageType.ITEM_UPDATE;
        itemService.updateItem(updated);
        setStatus("⏳ Đang gửi...", true);
    }

    @FXML
    public void handleDelete() {
        if (selectedItem == null) {
            setStatus("⚠ Hãy chọn sản phẩm cần xóa.", false);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xóa sản phẩm \"" + selectedItem.getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                pendingRequest = MessageType.ITEM_DELETE;
                itemService.deleteItem(selectedItem.getId(), currentSellerId);
                setStatus("⏳ Đang xóa...", true);
            }
        });
    }

    @FXML
    public void handleCreateAuction() {
        if (selectedItem == null) {
            setStatus("⚠ Hãy chọn sản phẩm để tạo phiên đấu giá.", false);
            return;
        }
        if (!"APPROVED".equalsIgnoreCase(selectedItem.getStatus())) {
            setStatus("⚠ Sản phẩm \"" + selectedItem.getName() + "\" chưa được Admin duyệt ("
                    + selectedItem.getStatus() + "). Vui lòng chờ duyệt trước khi tạo phiên.", false);
            return;
        }
        try {
            double startingPrice = Double.parseDouble(startingPriceField.getText().trim());
            int durationHours    = Integer.parseInt(durationHoursField.getText().trim());

            if (startingPrice <= 0 || durationHours <= 0) {
                setStatus("⚠ Giá khởi điểm và thời gian phải lớn hơn 0.", false);
                return;
            }

            Auction auction = new Auction();
            auction.setId(UUID.randomUUID().toString());
            auction.setItemId(selectedItem.getId());
            auction.setSellerId(currentSellerId);
            auction.setStartingPrice(startingPrice);
            auction.setCurrentPrice(startingPrice);
            auction.setStartTime(LocalDateTime.now());
            auction.setEndTime(LocalDateTime.now().plusHours(durationHours));

            pendingRequest = MessageType.AUCTION_CREATE;
            auctionService.createAuction(auction);
            setStatus("⏳ Đang tạo phiên...", true);
        } catch (NumberFormatException e) {
            setStatus("⚠ Giá khởi điểm và thời gian phải là số.", false);
        }
    }

    @FXML
    public void handleRefresh() {
        itemService.getItemsBySeller(currentSellerId);
    }

    @FXML
    public void handleClear() {
        clearForm();
        selectedItem = null;
        itemTable.getSelectionModel().clearSelection();
        pendingRequest = null;
    }

    private void onMessage(Message msg) {
        switch (msg.getType()) {
            case ITEM_LIST -> {
                try {
                    List<Item> items = parseItemList(msg.getPayload());
                    Platform.runLater(() ->
                            itemTable.setItems(FXCollections.observableArrayList(items)));
                } catch (Exception e) {
                    System.err.println("❌ Lỗi parse ITEM_LIST: " + e.getMessage());
                }
            }
            case SUCCESS -> {
                // Chỉ phản ứng khi màn hình này đang chờ phản hồi
                if (pendingRequest != null) {
                    MessageType req = pendingRequest;
                    pendingRequest = null;
                    Platform.runLater(() -> {
                        String successMsg = switch (req) {
                            case ITEM_ADD       -> "✅ Sản phẩm đã gửi duyệt thành công.";
                            case ITEM_UPDATE    -> "✅ Cập nhật thành công.";
                            case ITEM_DELETE    -> "✅ Xóa thành công.";
                            case AUCTION_CREATE -> "✅ Phiên đấu giá đã được tạo!";
                            default             -> "✅ Thao tác thành công.";
                        };
                        setStatus(successMsg, true);
                        handleRefresh();
                    });
                }
            }
            // ── Nhận thông báo realtime khi Admin duyệt/từ chối ──────────────────
            case ITEM_APPROVE -> {
                try {
                    JsonObject obj = JsonParser.parseString(msg.getPayload()).getAsJsonObject();
                    String itemName = obj.has("itemName") ? obj.get("itemName").getAsString() : "sản phẩm";
                    Platform.runLater(() -> {
                        setStatus("✅ Sản phẩm \"" + itemName + "\" đã được Admin duyệt!", true);
                        handleRefresh(); // Reload để thấy status APPROVED
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> setStatus("✅ Một sản phẩm đã được duyệt.", true));
                }
            }
            case ITEM_REJECT -> {
                try {
                    JsonObject obj = JsonParser.parseString(msg.getPayload()).getAsJsonObject();
                    String itemName = obj.has("itemName") ? obj.get("itemName").getAsString() : "sản phẩm";
                    Platform.runLater(() -> {
                        setStatus("❌ Sản phẩm \"" + itemName + "\" đã bị Admin từ chối.", false);
                        handleRefresh();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> setStatus("❌ Một sản phẩm đã bị từ chối.", false));
                }
            }
            case ERROR -> {
                if (pendingRequest != null) {
                    pendingRequest = null;
                    try {
                        String err = JsonUtil.fromJson(msg.getPayload(), ErrPayload.class).error();
                        Platform.runLater(() -> setStatus("❌ Lỗi: " + err, false));
                    } catch (Exception e) {
                        Platform.runLater(() -> setStatus("❌ Có lỗi xảy ra.", false));
                    }
                }
            }
            // Bỏ qua tất cả các type khác
        }
    }

    private Item buildItemFromForm() {
        String category = categoryBox.getValue();
        Item item = switch (category) {
            case "ELECTRONICS" -> {
                Electronics e = new Electronics();
                e.setBrand(brandField.getText().trim());
                e.setWarrantyMonths(warrantyField.getText().trim());
                yield e;
            }
            case "ART" -> {
                Art a = new Art();
                a.setArtist(artistField.getText().trim());
                try { a.setYear(Integer.parseInt(artYearField.getText().trim())); }
                catch (NumberFormatException ignored) {}
                yield a;
            }
            default -> {
                Vehicle v = new Vehicle();
                try {
                    v.setKilometre(Integer.parseInt(kmField.getText().trim()));
                    v.setManufacturingYear(Integer.parseInt(mfYearField.getText().trim()));
                } catch (NumberFormatException ignored) {}
                yield v;
            }
        };
        item.setName(nameField.getText().trim());
        item.setDescription(descField.getText().trim());
        item.setCategory(category);
        item.setSellerId(currentSellerId);
        item.setStatus("PENDING");
        if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
            item.setImagePath(selectedImagePath);
        }
        return item;
    }

    private void clearForm() {
        nameField.clear(); descField.clear();
        brandField.clear(); warrantyField.clear();
        artistField.clear(); artYearField.clear();
        kmField.clear(); mfYearField.clear();
        startingPriceField.clear(); durationHoursField.clear();
        selectedImagePath = null;
        imagePathLabel.setText("Chưa chọn ảnh");
    }

    private void setStatus(String msg, boolean ok) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (ok ? "#27ae60" : "#e74c3c") + ";");
    }

    private record ErrPayload(String error) {}

    private List<Item> parseItemList(String payload) {
        List<Item> result = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(payload).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String category = obj.has("category") ?
                        obj.get("category").getAsString() : "VEHICLE";
                Class<? extends Item> clazz = switch (category.toUpperCase()) {
                    case "ELECTRONICS" -> Electronics.class;
                    case "ART"         -> Art.class;
                    default            -> Vehicle.class;
                };
                result.add(JsonUtil.fromJson(obj.toString(), clazz));
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi parse Item List: " + e.getMessage());
        }
        return result;
    }
}
