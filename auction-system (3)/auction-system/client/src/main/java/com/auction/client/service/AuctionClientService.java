package com.auction.client.service;

import com.auction.client.network.SocketClient;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.Auction;

public class AuctionClientService {
    private final SocketClient socketClient = SocketClient.getInstance();

    public void login(String username, String passwordHash) {
        String payload = "{\"username\":\"" + username + "\",\"passwordHash\":\"" + passwordHash + "\"}";
        socketClient.send(new Message(MessageType.LOGIN, username, payload));
    }

    public void requestAuctionList(String userId) {
        socketClient.send(new Message(MessageType.AUCTION_LIST, userId, "{}"));
    }

    public void placeBid(String auctionId, String bidderId, double amount) {
        String payload = JsonUtil.toJson(new BidRequest(auctionId, bidderId, amount));
        socketClient.send(new Message(MessageType.BID_PLACE, bidderId, payload));
    }

    public void createAuction(Auction auction) {
        socketClient.send(new Message(MessageType.AUCTION_CREATE,
                auction.getSellerId(), JsonUtil.toJson(auction)));
    }

    private record BidRequest(String auctionId, String bidderId, double amount) {}
}
