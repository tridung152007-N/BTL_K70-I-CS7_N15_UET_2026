package com.auction.client.service;

import com.auction.client.network.SocketClient;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;

public class AutoBidClientService {
    private final SocketClient socketClient = SocketClient.getInstance();

    public void registerAutoBid(String auctionId, String bidderId, double maxBid, double increment) {
        String payload = "{\"auctionId\":\"" + auctionId + "\",\"bidderId\":\"" + bidderId
                + "\",\"maxBid\":" + maxBid + ",\"increment\":" + increment + "}";
        socketClient.send(new Message(MessageType.AUTO_BID_REGISTER, bidderId, payload));
    }

    public void cancelAutoBid(String auctionId, String bidderId) {
        socketClient.send(new Message(MessageType.AUTO_BID_CANCEL, bidderId,
                "{\"auctionId\":\"" + auctionId + "\"}"));
    }
}
