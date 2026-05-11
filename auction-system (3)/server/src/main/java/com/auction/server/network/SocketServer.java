package com.auction.server.network;

import com.auction.common.util.Constants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Singleton – lắng nghe kết nối từ client. */
public class SocketServer {
    private static SocketServer instance;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final BroadcastManager broadcastManager;
    private final MessageDispatcher dispatcher;
    private boolean running = false;

    private SocketServer(BroadcastManager broadcastManager, MessageDispatcher dispatcher) {
        this.broadcastManager = broadcastManager;
        this.dispatcher = dispatcher;
    }

    public static synchronized SocketServer getInstance(
            BroadcastManager broadcastManager, MessageDispatcher dispatcher) {
        if (instance == null) instance = new SocketServer(broadcastManager, dispatcher);
        return instance;
    }

    public void start() throws IOException {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT)) {
            System.out.println("Server started on port " + Constants.SERVER_PORT);
            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, broadcastManager, dispatcher);
                broadcastManager.register(handler);
                threadPool.submit(handler);
            }
        }
    }

    public void stop() { running = false; threadPool.shutdown(); }
}
