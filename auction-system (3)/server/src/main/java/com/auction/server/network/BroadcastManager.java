package com.auction.server.network;

import com.auction.common.network.Message;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Quản lý broadcast tới tất cả client đang kết nối – thread-safe. */
public class BroadcastManager {
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public void register(ClientHandler handler) { clients.add(handler); }

    public void unregister(ClientHandler handler) { clients.remove(handler); }

    public void broadcast(Message message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void broadcastToAuction(String auctionId, Message message) {
        // TODO: lọc chỉ những client đang xem auctionId
        broadcast(message);
    }

    public int getConnectedCount() { return clients.size(); }
}
