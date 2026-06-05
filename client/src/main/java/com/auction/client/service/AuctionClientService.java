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

    public boolean login(String username, String password) {
        String payload = JsonUtil.toJson(new LoginRequest(username, password));
        System.out.println("ðŸ“¤ CLIENT gá»­i LOGIN - Username: " + username);
        return socketClient.send(new Message(MessageType.LOGIN, username, payload));
    }
    public void requestBidHistory(String auctionId) {
        String payload = "{\"auctionId\":\"" + auctionId + "\"}";
        socketClient.send(new Message(MessageType.BID_HISTORY, "client", payload));
    }

    public void requestWalletBalance(String userId) {
        String payload = "{\"userId\":\"" + userId + "\"}";
        socketClient.send(new Message(MessageType.WALLET_BALANCE, userId, payload));
    }

    public void topUpWallet(String userId, double amount) {
        String payload = JsonUtil.toJson(new WalletRequest(userId, amount));
        socketClient.send(new Message(MessageType.WALLET_TOP_UP, userId, payload));
    }

    public void payAuction(String auctionId, String userId) {
        String payload = JsonUtil.toJson(new PaymentRequest(auctionId, userId));
        socketClient.send(new Message(MessageType.PAYMENT_PAY, userId, payload));
    }

    private record LoginRequest(String username, String password) {}

    public void requestAuctionList(String userId) {
        System.out.println("ðŸ“¤ CLIENT gá»­i AUCTION_LIST request userId=" + userId + " | connected=" + (socketClient != null));
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
    private record WalletRequest(String userId, double amount) {}
    private record PaymentRequest(String auctionId, String userId) {}
}
