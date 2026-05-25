package com.auction.client.chart;

import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import com.auction.server.model.BidTransaction;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicInteger;

public class BidHistoryChart {

    private final LineChart<Number, Number> chart;
    private final XYChart.Series<Number, Number> series;
    private final AtomicInteger bidIndex = new AtomicInteger(0);

    public BidHistoryChart() {
        NumberAxis xAxis = new NumberAxis(0, 20, 1);
        NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("Lần đấu");
        yAxis.setLabel("Giá (VNĐ)");
        yAxis.setAutoRanging(true);

        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("📈 Lịch sử giá đấu");
        chart.setAnimated(true);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(false);

        series = new XYChart.Series<>();
        series.setName("Giá đấu");
        chart.getData().add(series);

        System.out.println("✅ BidHistoryChart đã khởi tạo");
    }

    public void addBidPoint(BidTransaction bid) {
        if (bid == null) return;

        int index = bidIndex.incrementAndGet();
        double amount = bid.getAmount();
        String bidderId = bid.getBidderId() != null ? bid.getBidderId() : "Unknown";

        System.out.println("📊 Thêm bid: #" + index + " - " + bidderId + " - " + amount);

        Platform.runLater(() -> {
            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(index, amount);
            series.getData().add(dataPoint);

            // === TOOLTIP HIỂN THỊ THÔNG TIN CHI TIẾT ===
            String tooltipText = String.format(
                    "Lần đấu: %d\n" +
                            "Giá: %,.0f VNĐ\n" +
                            "Người đấu: %s",
                    index, amount, bidderId
            );

            Tooltip tooltip = new Tooltip(tooltipText);
            tooltip.setShowDelay(Duration.millis(50));
            tooltip.setHideDelay(Duration.millis(200));
            tooltip.setStyle("-fx-font-size: 13px; -fx-background-color: #2c3e50; -fx-text-fill: white;");

            // Gắn tooltip vào điểm trên biểu đồ
            if (dataPoint.getNode() != null) {
                Tooltip.install(dataPoint.getNode(), tooltip);
            }

            // Giới hạn số điểm
            if (series.getData().size() > 15) {
                series.getData().remove(0);
            }

            // Tự động scale trục Y
            if (chart.getYAxis() instanceof NumberAxis yAxis) {
                yAxis.setAutoRanging(true);
            }
        });
    }

    public LineChart<Number, Number> getChart() {
        return chart;
    }

    public void clear() {
        Platform.runLater(() -> {
            series.getData().clear();
            bidIndex.set(0);
        });
    }
}