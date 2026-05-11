package com.auction.client.network;

import com.auction.common.network.Message;

@FunctionalInterface
public interface MessageListener {
    void onMessageReceived(Message message);
}
