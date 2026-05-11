package com.auction.client.service;

import com.auction.client.network.SocketClient;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.Item;

public class ItemClientService {
    private final SocketClient socket = SocketClient.getInstance();

    public void addItem(Item item) {
        socket.send(new Message(MessageType.ITEM_ADD, item.getSellerId(), JsonUtil.toJson(item)));
    }

    public void updateItem(Item item) {
        socket.send(new Message(MessageType.ITEM_UPDATE, item.getSellerId(), JsonUtil.toJson(item)));
    }

    public void deleteItem(String itemId, String sellerId) {
        socket.send(new Message(MessageType.ITEM_DELETE, sellerId,
                "{\"itemId\":\"" + itemId + "\"}"));
    }

    public void getItemsBySeller(String sellerId) {
        socket.send(new Message(MessageType.ITEM_LIST, sellerId,
                "{\"sellerId\":\"" + sellerId + "\"}"));
    }
}
