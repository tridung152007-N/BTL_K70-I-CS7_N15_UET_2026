package com.auction.server;

import com.auction.server.dao.impl.*;
import com.auction.server.network.*;
import com.auction.server.service.*;
import com.auction.server.util.AuctionTimer;

public class ServerApp {
    public static void main(String[] args) throws Exception {
        // ── DAO ──
        var userDAO    = new UserDAOImpl();
        var auctionDAO = new AuctionDAOImpl();
        var itemDAO    = new ItemDAOImpl();
        var bidDAO     = new BidTransactionDAOImpl();

        // ── Service ──
        var userService    = new UserService(userDAO);
        var auctionService = new AuctionService(auctionDAO, bidDAO);
        var autoBidService = new AutoBidService(auctionService);
        var itemService    = new ItemService(itemDAO);

        var auctionTimer   = new AuctionTimer(auctionService);

        // ── Network ──
        var broadcastManager = new BroadcastManager();
        var dispatcher       = new MessageDispatcher(userService, auctionService,
                                                     autoBidService, itemService,broadcastManager,auctionTimer);

        System.out.println("🚀 Server đang khởi động...");
        SocketServer.getInstance(broadcastManager, dispatcher).start();
    }
}
