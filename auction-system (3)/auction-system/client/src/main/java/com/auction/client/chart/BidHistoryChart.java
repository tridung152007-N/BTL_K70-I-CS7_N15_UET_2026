package com.auction.client.chart;

import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import com.auction.server.model.BidTransaction;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Biểu đồ đường realtime – tự cập nhật mỗi khi có bid mới.
 * Dùng Platform.runLater() để đảm bảo update trên JavaFX thread.
 */
public class BidHistoryChart {
    private final LineChart<Number, Number> chart;
    private final XYChart.Series<Number, Number> series;
    private final AtomicInteger bidIndex = new AtomicInteger(0);

    public BidHistoryChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Lần đấu");
        yAxis.setLabel("Giá (VNĐ)");
        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Lịch sử giá đấu");
        chart.setAnimated(false);
        series = new XYChart.Series<>();
        series.setName("Giá cao nhất");
        chart.getData().add(series);
    }

    public void addBidPoint(BidTransaction bid) {
        int index = bidIndex.incrementAndGet();
        Platform.runLater(() ->
                series.getData().add(new XYChart.Data<>(index, bid.getAmount())));
    }

    public LineChart<Number, Number> getChart() { return chart; }

    public void clear() {
        Platform.runLater(() -> {
            series.getData().clear();
            bidIndex.set(0);
        });
    }
}
