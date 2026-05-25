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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import com.auction.server.util.AuctionTimer;

// Thêm vào danh sách import hiện có

public class MessageDispatcher {

    private final UserService    userService;
    private final AuctionService auctionService;
    private final AutoBidService autoBidService;
    private final ItemService    itemService;
    private final BroadcastManager broadcastManager;
    private final AuctionTimer auctionTimer;

    public MessageDispatcher(UserService userService, AuctionService auctionService,
                             AutoBidService autoBidService, ItemService itemService,BroadcastManager broadcastManager,AuctionTimer auctionTimer) {
        this.userService    = userService;
        this.auctionService = auctionService;
        this.autoBidService = autoBidService;
        this.itemService    = itemService;
        this.broadcastManager  = broadcastManager;
        this.auctionTimer   = auctionTimer;
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
                case ITEM_PENDING_LIST -> handleItemPendingList();
                case ITEM_APPROVE      -> handleItemApprove(msg);
                case ITEM_REJECT       -> handleItemReject(msg);
                case PING              -> ok("{\"pong\":true}");
                default                -> error("Không hỗ trợ: " + msg.getType());
            };
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    // ── Auth ──────────────────────────────────────────────
    private Message handleLogin(Message msg) {
        try {
            // Debug raw payload
            System.out.println("🔍 RAW PAYLOAD từ client: " + msg.getPayload());

            LoginReq req = JsonUtil.fromJson(msg.getPayload(), LoginReq.class);

            System.out.println("🔐 LOGIN - Username: " + req.username());
            System.out.println("🔐 LOGIN - Password nhận được: '" + req.password() + "'");

            Optional<User> userOpt = userService.login(req.username(), req.password());

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("🎉 ĐĂNG NHẬP THÀNH CÔNG: " + user.getUsername() + " - " + user.getRole());
                return ok(JsonUtil.toJson(user));
            } else {
                System.out.println("❌ Đăng nhập thất bại");
                return error("Sai tên đăng nhập hoặc mật khẩu");
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi handleLogin: " + e.getMessage());
            e.printStackTrace();
            return error("Lỗi server: " + e.getMessage());
        }
    }

    private Message handleRegister(Message msg) {
        RegisterReq req = JsonUtil.fromJson(msg.getPayload(), RegisterReq.class);

        User newUser = switch (req.role().toUpperCase()) {
            case "BIDDER" -> new Bidder(UUID.randomUUID().toString(),
                    req.username(), "", req.email());  // passwordHash tạm rỗng

            case "SELLER" -> new Seller(UUID.randomUUID().toString(),
                    req.username(), "", req.email(), req.shopName());

            default -> throw new IllegalArgumentException("Role không hợp lệ");
        };

        // Gọi register với plain password
        userService.register(newUser, req.passwordHash());  // passwordHash ở đây thực chất là plain password

        return ok("{\"status\":\"registered\"}");
    }

    private Message handleAuctionList() {
        // Lấy TẤT CẢ auction thay vì chỉ RUNNING
        List<Auction> auctions = auctionService.getAllAuctions();

        // Enrich thông tin sản phẩm
        for (Auction a : auctions) {
            try {
                Item item = itemService.getById(a.getItemId());
                a.setItemName(item.getName());
                a.setItemImagePath(item.getImagePath());
            } catch (Exception e) {
                a.setItemName("Sản phẩm #" + a.getItemId());
            }
        }

        return new Message(MessageType.AUCTION_LIST, "SERVER", JsonUtil.toJson(auctions));
    }

    private Message handleAuctionDetail(Message msg) {
        AuctionDetailReq req = JsonUtil.fromJson(msg.getPayload(), AuctionDetailReq.class);
        return ok(JsonUtil.toJson(auctionService.getById(req.auctionId())));
    }

    private Message handleAuctionCreate(Message msg) {
        Auction auction = JsonUtil.fromJson(msg.getPayload(), Auction.class);
        if (auction.getId() == null) auction.setId(UUID.randomUUID().toString());

        auction.setState(AuctionState.RUNNING);
        auction.setStartTime(LocalDateTime.now());

        // Tính thời gian kết thúc (nếu client chưa truyền)
        if (auction.getEndTime() == null) {
            auction.setEndTime(LocalDateTime.now().plusHours(2)); // mặc định 2 giờ
        }

        auctionService.createAuction(auction);

        // === SCHEDULE TIMER TỰ ĐỘNG ĐÓNG ===
        long delaySeconds = ChronoUnit.SECONDS.between(
                LocalDateTime.now(), auction.getEndTime());

        auctionTimer.schedule(auction.getId(), delaySeconds);

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
    private Message handleItemAdd(Message msg) {
        try {
            System.out.println("🔥🔥🔥 [ITEM_ADD] NHẬN ĐƯỢC TỪ SELLER: " + msg.getSenderId());
            System.out.println("   Full Payload: " + msg.getPayload());

            Class<? extends Item> clazz = resolveItemClass(msg.getPayload());
            System.out.println("📌 Parse dùng class: " + clazz.getSimpleName());

            Item item = JsonUtil.fromJson(msg.getPayload(), clazz);

            System.out.println("✅ Parse JSON OK → Tên: " + item.getName()
                    + " | Category: " + item.getCategory()
                    + " | Status: " + item.getStatus());

            if (item.getId() == null) item.setId(UUID.randomUUID().toString());
            item.setSellerId(msg.getSenderId());

            itemService.addItem(item);

            System.out.println("🎉🎉🎉 LƯU THÀNH CÔNG VÀO DATABASE! ID = " + item.getId());

            return ok("{\"status\":\"item_added\",\"itemId\":\"" + item.getId() + "\"}");

        } catch (Exception e) {
            System.err.println("❌ LỖI handleItemAdd: " + e.getMessage());
            e.printStackTrace();
            return error("Lỗi thêm sản phẩm: " + e.getMessage());
        }
    }
    private Message handleItemUpdate(Message msg) {
        Item item = JsonUtil.fromJson(msg.getPayload(), resolveItemClass(msg.getPayload()));
        itemService.updateItem(item);

        broadcastPendingItems();           // ← gọi broadcast

        return ok("{\"status\":\"item_updated\"}");
    }

    private void broadcastPendingItems() {
        try {
            List<Item> pending = itemService.getPendingItems();
            System.out.println("📢 Broadcast " + pending.size() + " sản phẩm pending cho Admin");

            Message msg = new Message(MessageType.ITEM_PENDING_LIST, "SERVER", JsonUtil.toJson(pending));
            broadcastManager.broadcast(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private Message handleItemPendingList() {
        List<Item> items = itemService.getPendingItems();
        return new Message(MessageType.ITEM_PENDING_LIST, "SERVER", JsonUtil.toJson(items));
    }

    private Message handleItemApprove(Message msg) {
        ItemActionReq req = JsonUtil.fromJson(msg.getPayload(), ItemActionReq.class);
        itemService.approveItem(req.itemId());
        return ok("{\"status\":\"approved\",\"itemId\":\"" + req.itemId() + "\"}");
    }

    private Message handleItemReject(Message msg) {
        ItemActionReq req = JsonUtil.fromJson(msg.getPayload(), ItemActionReq.class);
        itemService.rejectItem(req.itemId());
        return ok("{\"status\":\"rejected\",\"itemId\":\"" + req.itemId() + "\"}");
    }

    // ── Helper ────────────────────────────────────────────
    private Class<? extends Item> resolveItemClass(String payload) {
        if (payload == null) return Vehicle.class;

        String upper = payload.toUpperCase();

        if (upper.contains("ELECTRONICS")) {
            return Electronics.class;
        }
        if (upper.contains("ART")) {
            return Art.class;
        }
        return Vehicle.class;   // default
    }

    private Message ok(String payload) {
        return new Message(MessageType.SUCCESS, "SERVER", payload);
    }

    private Message error(String reason) {
        return new Message(MessageType.ERROR, "SERVER", "{\"error\":\"" + reason + "\"}");
    }

    // ── Request DTOs ──────────────────────────────────────
    private record LoginReq(String username, String password) {}
    private record RegisterReq(String username, String passwordHash,
                               String email, String role, String shopName) {}
    private record BidReq(String auctionId, String bidderId, double amount) {}
    private record AutoBidReq(String bidderId, String auctionId, double maxBid, double increment) {}
    private record AutoBidCancelReq(String auctionId, String bidderId) {}
    private record AuctionDetailReq(String auctionId) {}
    private record DeleteReq(String itemId) {}
    private record SellerReq(String sellerId) {}
    private record ItemActionReq(String itemId) {}
}
