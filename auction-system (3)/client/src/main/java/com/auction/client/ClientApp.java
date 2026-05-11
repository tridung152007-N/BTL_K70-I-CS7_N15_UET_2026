package com.auction.client;

import com.auction.client.network.SocketClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        SocketClient.getInstance().connect();
        FXMLLoader loader = new FXMLLoader(
                ClientApp.class.getResource("/com/auction/client/fxml/Login.fxml"));
        Scene scene = new Scene(loader.load(), 400, 300);
        stage.setTitle("Hệ thống đấu giá trực tuyến");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        SocketClient.getInstance().disconnect();
    }

    public static void main(String[] args) { launch(args); }
}
