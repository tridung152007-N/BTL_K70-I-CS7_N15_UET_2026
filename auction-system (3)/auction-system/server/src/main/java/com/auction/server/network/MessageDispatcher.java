package com.auction.server.network;

import com.auction.common.model.AuctionState;
import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.model.*;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.ItemService;
import com.auction.server.service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MessageDispatcher {

    private final UserService    userService;
    private final AuctionService auctionService;
    private final AutoBidService autoBidService;
    private final ItemService    itemService;

    public MessageDispatcher(UserService userService, AuctionService auctionService,
                             AutoBidService autoBidService, ItemService itemService) {
        this.userService    = userService;
        this.auctionService = auctionService;
        this.autoBidService = autoBidService;
        this.itemService    = itemService;
    }

    public Message dispatch(Message msg, ClientHandler handler) {
        try {
            return switch (msg.getType()) {
                case LOGIN             -> handleLogin(msg);
                case REGISTER          -> handleRegister(msg);
                case AUCTION_LIST      -> handleAuctionList();
                case AUCTION_DETAIL    -> handleAuctionDetail(msg);
                case AUCTION_CREATE    -> handleAuctionCreate(msg);
                case BID_PLACE         -> handleBid(msg);
                case AUTO_BID_REGISTER -> handleAutoBidRegister(msg);
                case AUTO_BID_CANCEL   -> handleAutoBidCancel(msg);
                case ITEM_ADD          -> handleItemAdd(msg);
                case ITEM_UPDATE       -> handleItemUpdate(msg);
                case ITEM_DELETE       -> handleItemDelete(msg);
                case ITEM_LIST         -> handleItemList(msg);
                case PING              -> ok("{\"pong\":true}");
                default                -> error("Không hỗ trợ: " + msg.getType());
            };
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    // ── Auth ──────────────────────────────────────────────
    private Message handleLogin(Message msg) {
        LoginReq req = JsonUtil.fromJson(msg.getPayload(), LoginReq.class);
        Optional<User> user = userService.login(req.username(), req.passwordHash());
        return user.isPresent() ? ok(JsonUtil.toJson(user.get()))
                                : error("Sai tên đăng nhập hoặc mật khẩu");
    }

    private Message handleRegister(Message msg) {
        RegisterReq req = JsonUtil.fromJson(msg.getPayload(), RegisterReq.class);
        User newUser = switch (req.role().toUpperCase()) {
            case "BIDDER" -> new Bidder(UUID.randomUUID().toString(),
                    req.username(), req.passwordHash(), req.email());
            case "SELLER" -> new Seller(UUID.randomUUID().toString(),
                    req.username(), req.passwordHash(), req.email(), req.shopName());
            default -> throw new IllegalArgumentException("Role không hợp lệ");
        };
        userService.register(newUser);
        return ok("{\"status\":\"registered\"}");
    }

    // ── Auction ───────────────────────────────────────────
    private Message handleAuctionList() {
        List<Auction> list = auctionService.getRunningAuctions();
        return new Message(MessageType.AUCTION_LIST, "SERVER", JsonUtil.toJson(list));
    }

    private Message handleAuctionDetail(Message msg) {
        AuctionDetailReq req = JsonUtil.fromJson(msg.getPayload(), AuctionDetailReq.class);
        return ok(JsonUtil.toJson(auctionService.getById(req.auctionId())));
    }

    private Message handleAuctionCreate(Message msg) {
        Auction auction = JsonUtil.fromJson(msg.getPayload(), Auction.class);
        if (auction.getId() == null) auction.setId(UUID.randomUUID().toString());
        auction.setState(AuctionState.RUNNING);
        auctionService.createAuction(auction);
        return ok("{\"status\":\"auction_created\",\"auctionId\":\"" + auction.getId() + "\"}");
    }

    // ── Bid ───────────────────────────────────────────────
    private Message handleBid(Message msg) {
        BidReq req = JsonUtil.fromJson(msg.getPayload(), BidReq.class);
        BidTransaction bid = auctionService.placeBid(req.auctionId(), req.bidderId(), req.amount());
        return ok(JsonUtil.toJson(bid));
    }

    // ── Auto-bid ──────────────────────────────────────────
    private Message handleAutoBidRegister(Message msg) {
        AutoBidReq req = JsonUtil.fromJson(msg.getPayload(), AutoBidReq.class);
        autoBidService.registerAutoBid(new AutoBidConfig(
                req.bidderId(), req.auctionId(), req.maxBid(), req.increment()));
        return ok("{\"status\":\"auto_bid_registered\"}");
    }

    private Message handleAutoBidCancel(Message msg) {
        AutoBidCancelReq req = JsonUtil.fromJson(msg.getPayload(), AutoBidCancelReq.class);
        autoBidService.cancelAutoBid(req.auctionId(), req.bidderId());
        return ok("{\"status\":\"auto_bid_canceled\"}");
    }

    // ── Item (Seller) ─────────────────────────────────────
    private Message handleItemAdd(Message msg) {
        Item item = JsonUtil.fromJson(msg.getPayload(), resolveItemClass(msg.getPayload()));
        if (item.getId() == null) item.setId(UUID.randomUUID().toString());
        itemService.addItem(item);
        return ok("{\"status\":\"item_added\",\"itemId\":\"" + item.getId() + "\"}");
    }

    private Message handleItemUpdate(Message msg) {
        Item item = JsonUtil.fromJson(msg.getPayload(), resolveItemClass(msg.getPayload()));
        itemService.updateItem(item);
        return ok("{\"status\":\"item_updated\"}");
    }

    private Message handleItemDelete(Message msg) {
        DeleteReq req = JsonUtil.fromJson(msg.getPayload(), DeleteReq.class);
        itemService.deleteItem(req.itemId());
        return ok("{\"status\":\"item_deleted\"}");
    }

    private Message handleItemList(Message msg) {
        SellerReq req = JsonUtil.fromJson(msg.getPayload(), SellerReq.class);
        List<Item> items = itemService.getItemsBySeller(req.sellerId());
        return new Message(MessageType.ITEM_LIST, "SERVER", JsonUtil.toJson(items));
    }

    // ── Helper ────────────────────────────────────────────
    private Class<? extends Item> resolveItemClass(String payload) {
        if (payload.contains("\"ELECTRONICS\"")) return Electronics.class;
        if (payload.contains("\"ART\""))         return Art.class;
        return Vehicle.class;
    }

    private Message ok(String payload) {
        return new Message(MessageType.SUCCESS, "SERVER", payload);
    }

    private Message error(String reason) {
        return new Message(MessageType.ERROR, "SERVER", "{\"error\":\"" + reason + "\"}");
    }

    // ── Request DTOs ──────────────────────────────────────
    private record LoginReq(String username, String passwordHash) {}
    private record RegisterReq(String username, String passwordHash,
                                String email, String role, String shopName) {}
    private record BidReq(String auctionId, String bidderId, double amount) {}
    private record AutoBidReq(String bidderId, String auctionId, double maxBid, double increment) {}
    private record AutoBidCancelReq(String auctionId, String bidderId) {}
    private record AuctionDetailReq(String auctionId) {}
    private record DeleteReq(String itemId) {}
    private record SellerReq(String sellerId) {}
}
