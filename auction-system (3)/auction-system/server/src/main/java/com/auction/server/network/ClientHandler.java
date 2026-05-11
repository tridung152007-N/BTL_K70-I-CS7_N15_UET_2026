package com.auction.server.network;

import com.auction.common.network.Message;
import com.auction.common.util.JsonUtil;
import com.auction.server.observer.AuctionObserver;
import com.auction.server.model.BidTransaction;

import java.io.*;
import java.net.Socket;

/** Mỗi client kết nối → một ClientHandler chạy trên thread riêng. */
public class ClientHandler implements Runnable, AuctionObserver {
    private final Socket socket;
    private final BroadcastManager broadcastManager;
    private final MessageDispatcher dispatcher;
    private PrintWriter out;
    private String clientId;

    public ClientHandler(Socket socket, BroadcastManager broadcastManager,
                         MessageDispatcher dispatcher) {
        this.socket = socket;
        this.broadcastManager = broadcastManager;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            this.out = writer;
            String line;
            while ((line = in.readLine()) != null) {
                Message msg = JsonUtil.fromJson(line, Message.class);
                this.clientId = msg.getSenderId();
                Message response = dispatcher.dispatch(msg, this);
                sendMessage(response);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + clientId);
        } finally {
            broadcastManager.unregister(this);
        }
    }

    public void sendMessage(Message msg) {
        if (out != null) out.println(JsonUtil.toJson(msg));
    }

    public String getClientId() { return clientId; }

    @Override
    public void onBidUpdate(String auctionId, BidTransaction bid) {
        // Push realtime update đến client này
        Message msg = new Message(
                com.auction.common.network.MessageType.BID_UPDATE,
                "SERVER",
                JsonUtil.toJson(bid));
        sendMessage(msg);
    }

    @Override
    public void onAuctionEnd(String auctionId, String winnerId, double finalPrice) {
        Message msg = new Message(
                com.auction.common.network.MessageType.AUCTION_END,
                "SERVER",
                "{\"auctionId\":\"" + auctionId + "\",\"winnerId\":\"" + winnerId
                        + "\",\"finalPrice\":" + finalPrice + "}");
        sendMessage(msg);
    }
}
