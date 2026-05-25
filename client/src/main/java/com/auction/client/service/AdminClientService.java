package com.auction.client.service;

import com.auction.client.network.SocketClient;
import com.auction.client.session.UserSession;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;

public class AdminClientService {
    private final SocketClient socket = SocketClient.getInstance();

    public void getPendingItems() {
        socket.send(new Message(MessageType.ITEM_PENDING_LIST,
                UserSession.getInstance().getUserId(), "{}"));
    }

    public void approveItem(String itemId) {
        socket.send(new Message(MessageType.ITEM_APPROVE,
                UserSession.getInstance().getUserId(),
                "{\"itemId\":\"" + itemId + "\"}"));
    }

    public void rejectItem(String itemId) {
        socket.send(new Message(MessageType.ITEM_REJECT,
                UserSession.getInstance().getUserId(),
                "{\"itemId\":\"" + itemId + "\"}"));
    }
}
