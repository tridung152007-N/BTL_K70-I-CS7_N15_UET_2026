module com.auction.auctionsystem {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.auction to javafx.fxml;
    exports com.auction;
    exports com.auction.controller;
    opens com.auction.controller to javafx.fxml;
    exports com.auction.app;
    opens com.auction.app to javafx.fxml;
}