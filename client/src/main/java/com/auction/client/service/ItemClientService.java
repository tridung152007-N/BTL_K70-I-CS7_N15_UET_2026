package com.auction.client.service;

import com.auction.client.network.SocketClient;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.Item;

public class ItemClientService {
    private final SocketClient socket = SocketClient.getInstance();

    public void addItem(Item item) {
        try {
            String payload = JsonUtil.toJson(item);
            System.out.println("📤 GỬI PAYLOAD: " + payload);

            Message msg = new Message(MessageType.ITEM_ADD, item.getSellerId(), payload);
            socket.send(msg);

            System.out.println("✅ ĐÃ GỬI Message ITEM_ADD thành công");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Lỗi gửi addItem: " + e.getMessage());
        }
    }

    public void updateItem(Item item) {
        try {
            String payload = JsonUtil.toJson(item);
            Message msg = new Message(MessageType.ITEM_UPDATE, item.getSellerId(), payload);
            socket.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteItem(String itemId, String sellerId) {
        try {
            String payload = "{\"itemId\":\"" + itemId + "\"}";
            Message msg = new Message(MessageType.ITEM_DELETE, sellerId, payload);
            socket.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getItemsBySeller(String sellerId) {
        try {
            String payload = "{\"sellerId\":\"" + sellerId + "\"}";
            Message msg = new Message(MessageType.ITEM_LIST, sellerId, payload);
            socket.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}