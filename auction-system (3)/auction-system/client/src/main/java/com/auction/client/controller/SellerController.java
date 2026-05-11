package com.auction.client.controller;

import com.auction.client.network.SocketClient;
import com.auction.client.service.AuctionClientService;
import com.auction.client.service.ItemClientService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

    private final ItemClientService itemService     = new ItemClientService();
    private final AuctionClientService auctionService = new AuctionClientService();
    private String currentSellerId = "seller1"; // TODO: inject từ login
    private Item selectedItem = null;

    @FXML
    public void initialize() {
        // Cài cột bảng
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty("Chưa đấu giá"));

        // ComboBox danh mục
        categoryBox.setItems(FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));
        categoryBox.setValue("ELECTRONICS");
        categoryBox.setOnAction(e -> showCategoryFields(categoryBox.getValue()));
        showCategoryFields("ELECTRONICS");

        // Chọn dòng → đổ dữ liệu vào form
        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            selectedItem = item;
            if (item != null) fillForm(item);
        });

        // Lắng nghe server
        SocketClient.getInstance().addListener(this::onMessage);
        handleRefresh();
    }

    // ── Hiển thị đúng nhóm field theo category ────────────
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

    // ── Đổ dữ liệu item vào form khi chọn dòng ───────────
    private void fillForm(Item item) {
        nameField.setText(item.getName());
        descField.setText(item.getDescription());
        categoryBox.setValue(item.getCategory());
        showCategoryFields(item.getCategory());
        if (item instanceof Electronics e) {
            brandField.setText(e.getBrand());
            warrantyField.setText(e.getWarrantyMonths());
        } else if (item instanceof Art a) {
            artistField.setText(a.getArtist());
            artYearField.setText(String.valueOf(a.getYear()));
        } else if (item instanceof Vehicle v) {
            kmField.setText(String.valueOf(v.getKilometre()));
            mfYearField.setText(String.valueOf(v.getManufacturingYear()));
        }
    }

    // ── Thêm sản phẩm mới ─────────────────────────────────
    @FXML
    public void handleAdd() {
        if (nameField.getText().trim().isEmpty()) {
            setStatus("⚠ Vui lòng nhập tên sản phẩm.", false);
            return;
        }
        Item item = buildItemFromForm();
        item.setId(UUID.randomUUID().toString());
        item.setSellerId(currentSellerId);
        itemService.addItem(item);
        setStatus("✅ Đã gửi yêu cầu thêm sản phẩm.", true);
        clearForm();
    }

    // ── Sửa sản phẩm đang chọn ────────────────────────────
    @FXML
    public void handleEdit() {
        if (selectedItem == null) {
            setStatus("⚠ Hãy chọn sản phẩm cần sửa.", false);
            return;
        }
        Item updated = buildItemFromForm();
        updated.setId(selectedItem.getId());
        updated.setSellerId(currentSellerId);
        itemService.updateItem(updated);
        setStatus("✅ Đã gửi yêu cầu cập nhật.", true);
    }

    // ── Xóa sản phẩm đang chọn ────────────────────────────
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
                itemService.deleteItem(selectedItem.getId(), currentSellerId);
                setStatus("✅ Đã gửi yêu cầu xóa.", true);
            }
        });
    }

    // ── Tạo phiên đấu giá cho sản phẩm đang chọn ─────────
    @FXML
    public void handleCreateAuction() {
        if (selectedItem == null) {
            setStatus("⚠ Hãy chọn sản phẩm để tạo phiên đấu giá.", false);
            return;
        }
        try {
            double startingPrice = Double.parseDouble(startingPriceField.getText().trim());
            int durationHours    = Integer.parseInt(durationHoursField.getText().trim());

            Auction auction = new Auction();
            auction.setId(UUID.randomUUID().toString());
            auction.setItemId(selectedItem.getId());
            auction.setSellerId(currentSellerId);
            auction.setStartingPrice(startingPrice);
            auction.setCurrentPrice(startingPrice);
            auction.setStartTime(LocalDateTime.now());
            auction.setEndTime(LocalDateTime.now().plusHours(durationHours));

            auctionService.createAuction(auction);
            setStatus("✅ Phiên đấu giá đã được tạo!", true);
        } catch (NumberFormatException e) {
            setStatus("⚠ Giá khởi điểm và thời gian phải là số.", false);
        }
    }

    // ── Tải lại danh sách sản phẩm ────────────────────────
    @FXML
    public void handleRefresh() {
        itemService.getItemsBySeller(currentSellerId);
    }

    // ── Xóa form ──────────────────────────────────────────
    @FXML
    public void handleClear() {
        clearForm();
        selectedItem = null;
        itemTable.getSelectionModel().clearSelection();
    }

    // ── Nhận message từ server ────────────────────────────
    private void onMessage(Message msg) {
        switch (msg.getType()) {
            case ITEM_LIST -> {
                List<Item> items = Arrays.asList(
                        JsonUtil.fromJson(msg.getPayload(), Item[].class));
                Platform.runLater(() ->
                        itemTable.setItems(FXCollections.observableArrayList(items)));
            }
            case SUCCESS -> Platform.runLater(() ->
                    setStatus("✅ Thao tác thành công.", true));
            case ERROR -> {
                String err = JsonUtil.fromJson(msg.getPayload(), ErrPayload.class).error();
                Platform.runLater(() -> setStatus("❌ Lỗi: " + err, false));
            }
        }
    }

    // ── Helper ────────────────────────────────────────────
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
        return item;
    }

    private void clearForm() {
        nameField.clear(); descField.clear();
        brandField.clear(); warrantyField.clear();
        artistField.clear(); artYearField.clear();
        kmField.clear(); mfYearField.clear();
        startingPriceField.clear(); durationHoursField.clear();
    }

    private void setStatus(String msg, boolean ok) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (ok ? "#27ae60" : "#e74c3c") + ";");
    }

    private record ErrPayload(String error) {}
}
