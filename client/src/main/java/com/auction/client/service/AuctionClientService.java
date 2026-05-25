package com.auction.client.service;

import com.auction.client.network.SocketClient;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.Auction;

public class    AuctionClientService {
    private final SocketClient socketClient = SocketClient.getInstance();

    public void register(String username, String password, String email,
                         String role, String shopName) {
        String payload = JsonUtil.toJson(new RegisterRequest(
                username, password, email, role, shopName));

        socketClient.send(new Message(MessageType.REGISTER, username, payload));
    }
    private record RegisterRequest(String username, String passwordHash,
                                   String email, String role, String shopName) {}

    public void login(String username, String password) {
        String payload = JsonUtil.toJson(new LoginRequest(username, password));
        System.out.println("📤 CLIENT gửi LOGIN - Username: " + username);
        socketClient.send(new Message(MessageType.LOGIN, username, payload));
    }

    private record LoginRequest(String username, String password) {}

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
