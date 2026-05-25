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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    private final ItemClientService itemService     = new ItemClientService();
    private final AuctionClientService auctionService = new AuctionClientService();
    private String currentSellerId;
    private Item selectedItem = null;
    private com.auction.client.network.MessageListener msgListener;
    private ScheduledExecutorService autoRefresh;
    private String selectedImagePath = null;
    @FXML
    public void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                // Tạo tên file unique
                String extension = file.getName().substring(file.getName().lastIndexOf("."));
                String newFileName = UUID.randomUUID() + extension;

                File destFolder = new File("uploads");
                if (!destFolder.exists()) destFolder.mkdirs();

                File destFile = new File(destFolder, newFileName);
                Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                selectedImagePath = "uploads/" + newFileName;
                imagePathLabel.setText(newFileName);

                // Preview
                // previewImage.setImage(new Image(file.toURI().toString()));

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

        // ==================== Listener chọn dòng ====================
        itemTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedItem = newValue;
            if (newValue != null) {
                System.out.println("✓ Đã chọn sản phẩm: " + newValue.getName()); // debug
                fillForm(newValue);
            } else {
                System.out.println("✓ Bỏ chọn sản phẩm");
            }
        });

        // Đăng ký listener message
        msgListener = this::onMessage;
        SocketClient.getInstance().addListener(msgListener);

        // Load dữ liệu lần đầu
        handleRefresh();

        // Auto refresh mỗi 5 giây
        autoRefresh = Executors.newSingleThreadScheduledExecutor();
        autoRefresh.scheduleAtFixedRate(this::handleRefresh, 5, 5, TimeUnit.SECONDS);
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

        System.out.println("📤 Seller gửi Item: " + item.getName()
                + " | Category=" + item.getCategory());

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
        if (!"APPROVED".equalsIgnoreCase(selectedItem.getStatus())) {
            setStatus("⚠ Sản phẩm \"" + selectedItem.getName() + "\" chưa được Admin duyệt ("
                    + selectedItem.getStatus() + "). Vui lòng chờ duyệt trước khi tạo phiên.", false);
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
                try {
                    List<Item> items = parseItemList(msg.getPayload());
                    Platform.runLater(() ->
                            itemTable.setItems(FXCollections.observableArrayList(items)));
                } catch (Exception e) {
                    System.err.println("❌ Lỗi parse ITEM_LIST: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            case SUCCESS -> Platform.runLater(() -> {
                setStatus("✅ Thao tác thành công.", true);
                handleRefresh();
            });
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
                try {
                    a.setYear(Integer.parseInt(artYearField.getText().trim()));
                } catch (NumberFormatException ignored) {}
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

        // Thông tin chung
        item.setName(nameField.getText().trim());
        item.setDescription(descField.getText().trim());
        item.setCategory(category);
        item.setSellerId(currentSellerId);
        item.setStatus("PENDING");

        // ✅ Xử lý ảnh (phải đặt TRƯỚC return)
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
    }

    private void setStatus(String msg, boolean ok) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: " + (ok ? "#27ae60" : "#e74c3c") + ";");
    }

    private record ErrPayload(String error) {}
    // ── Parse Item polymorphic (fix lỗi Abstract class) ─────────────────
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

                Item item = JsonUtil.fromJson(obj.toString(), clazz);
                result.add(item);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi parse Item List: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}
