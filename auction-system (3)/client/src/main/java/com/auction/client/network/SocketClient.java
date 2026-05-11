package com.auction.client.network;

import com.auction.common.network.Message;
import com.auction.common.util.Constants;
import com.auction.common.util.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/** Singleton – kết nối đến server và nhận message theo Thread riêng. */
public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private PrintWriter out;
    private final List<MessageListener> listeners = new ArrayList<>();

    private SocketClient() {}

    public static synchronized SocketClient getInstance() {
        if (instance == null) instance = new SocketClient();
        return instance;
    }

    public void connect() throws IOException {
        socket = new Socket(Constants.SERVER_HOST, Constants.SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        Thread listenerThread = new Thread(this::listenLoop);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenLoop() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = JsonUtil.fromJson(line, Message.class);
                for (MessageListener listener : new ArrayList<>(listeners)) {
                    listener.onMessageReceived(msg);
                }
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server");
        }
    }

    public void send(Message message) {
        if (out != null) out.println(JsonUtil.toJson(message));
    }

    public void addListener(MessageListener listener) { listeners.add(listener); }
    public void removeListener(MessageListener listener) { listeners.remove(listener); }

    public void disconnect() throws IOException { if (socket != null) socket.close(); }
}
