package com.auction.server.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.auction.common.network.Message;
import com.auction.common.network.MessageType;
import com.auction.common.util.JsonUtil;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.model.Art;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionItem;
import com.auction.server.model.BidTransaction;
import com.auction.server.model.Bidder;
import com.auction.server.model.Electronics;
import com.auction.server.model.Item;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.model.Vehicle;
import com.auction.server.service.AuctionItemService;
import com.auction.server.service.AuctionService;
import com.auction.server.service.ItemService;
import com.auction.server.service.UserService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Controller
public class AuctionWebSocketController {

    private final AuctionItemService auctionItemService;
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final BidTransactionDAO bidTransactionDAO;

    /**
     * Map: sessionId â†’ userId
     * DÃ¹ng Ä‘á»ƒ biáº¿t session nÃ o Ä‘ang giá»¯ bá»Ÿi user nÃ o,
     * tá»« Ä‘Ã³ gá»­i riÃªng message Ä‘áº¿n Ä‘Ãºng session.
     */
    private final ConcurrentHashMap<String, String> sessionUserMap = new ConcurrentHashMap<>();

    /**
     * Map: userId â†’ sessionId (nhiá»u tab/mÃ n hÃ¬nh cÃ¹ng 1 user láº¥y session má»›i nháº¥t)
     */
    private final ConcurrentHashMap<String, String> userSessionMap = new ConcurrentHashMap<>();

    public AuctionWebSocketController(AuctionItemService auctionItemService,
                                      ItemService itemService,
                                      AuctionService auctionService,
                                      UserService userService,
                                      SimpMessagingTemplate messagingTemplate,
                                      BidTransactionDAO bidTransactionDAO) {
        this.auctionItemService = auctionItemService;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
        this.bidTransactionDAO = bidTransactionDAO;
    }

    // â”€â”€ Session lifecycle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        System.out.println("ðŸ”Œ SESSION connected: " + sessionId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        String userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            userSessionMap.remove(userId);
            System.out.println("ðŸ”Œ SESSION disconnected: " + sessionId + " (user=" + userId + ")");
        }
    }

    // â”€â”€ Main handler â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @MessageMapping("/action")
    public void handleAction(@Payload String rawMessage, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String replyChannel = headerAccessor.getFirstNativeHeader("reply-to");
        if (replyChannel == null || replyChannel.isBlank()) replyChannel = sessionId;
        System.out.println("ðŸ“¨ SERVER handleAction sessionId=" + sessionId + " len=" + (rawMessage == null ? 0 : rawMessage.length()));

        try {
            Message msg = JsonUtil.fromJson(rawMessage, Message.class);
            System.out.println("ðŸ“© SERVER type=" + msg.getType() + " sender=" + msg.getSenderId());

            switch (msg.getType()) {
                case LOGIN -> {
                    LoginRequest req = JsonUtil.fromJson(msg.getPayload(), LoginRequest.class);
                    User user = userService.login(req.username(), req.password());

                    // Ghi nháº­n session â†” userId
                    String userId = user.getId();
                    sessionUserMap.put(sessionId, userId);
                    userSessionMap.put(userId, replyChannel);
                    System.out.println("âœ… LOGIN mapped sessionId=" + sessionId + " â†’ userId=" + userId);

                    replyTo(replyChannel, MessageType.SUCCESS, JsonUtil.toJson(user));
                }
                case REGISTER -> {
                    RegisterRequest req = JsonUtil.fromJson(msg.getPayload(), RegisterRequest.class);
                    User newUser;
                    if ("SELLER".equalsIgnoreCase(req.role())) {
                        Seller seller = new Seller();
                        seller.setShopName(req.shopName());
                        newUser = seller;
                    } else {
                        newUser = new Bidder();
                    }
                    newUser.setUsername(req.username());
                    newUser.setEmail(req.email());
                    newUser.setRole(req.role());
                    userService.register(newUser, req.passwordHash());
                    replyTo(replyChannel, MessageType.SUCCESS, "\"Dang ky thanh cong\"");
                }
                case WALLET_BALANCE -> {
                    UserIdPayload req = JsonUtil.fromJson(msg.getPayload(), UserIdPayload.class);
                    replyTo(replyChannel, MessageType.WALLET_BALANCE,
                            walletPayload(req.userId(), userService.getBalance(req.userId())));
                }
                case WALLET_TOP_UP -> {
                    WalletRequest req = JsonUtil.fromJson(msg.getPayload(), WalletRequest.class);
                    double balance = userService.topUp(req.userId(), req.amount());
                    replyTo(replyChannel, MessageType.WALLET_BALANCE,
                            walletPayload(req.userId(), balance));
                }
                case ITEM_LIST -> {
                    SellerPayload sp = JsonUtil.fromJson(msg.getPayload(), SellerPayload.class);
                    List<Item> items = itemService.getItemsBySeller(sp.sellerId());
                    replyTo(replyChannel, MessageType.ITEM_LIST, JsonUtil.toJson(items));
                }
                case ITEM_ADD -> {
                    Item item = parseItem(msg.getPayload());
                    if (item.getId() == null) item.setId(UUID.randomUUID().toString());
                    item.setStatus("PENDING");
                    itemService.addItem(item);
                    replyTo(replyChannel, MessageType.SUCCESS, "\"San pham da duoc gui duyet\"");

                    // Broadcast cho Admin biáº¿t cÃ³ sáº£n pháº©m má»›i chá» duyá»‡t
                    broadcastAdminPendingUpdate();
                }
                case ITEM_UPDATE -> {
                    Item item = parseItem(msg.getPayload());
                    itemService.updateItem(item);
                    replyTo(replyChannel, MessageType.SUCCESS, "\"Cap nhat thanh cong\"");
                }
                case ITEM_DELETE -> {
                    ItemIdPayload dp = JsonUtil.fromJson(msg.getPayload(), ItemIdPayload.class);
                    itemService.deleteItem(dp.itemId());
                    replyTo(replyChannel, MessageType.SUCCESS, "\"Xoa thanh cong\"");
                }
                case ITEM_PENDING_LIST -> {
                    List<Item> pending = itemService.getPendingItems();
                    replyTo(replyChannel, MessageType.ITEM_PENDING_LIST, JsonUtil.toJson(pending));
                }
                case ITEM_APPROVE -> {
                    ItemIdPayload ap = JsonUtil.fromJson(msg.getPayload(), ItemIdPayload.class);

                    // Láº¥y sellerId trÆ°á»›c khi approve Ä‘á»ƒ notify Ä‘Ãºng ngÆ°á»i
                    Item item = itemService.getById(ap.itemId());
                    String sellerId = item.getSellerId();
                    String itemName = item.getName();

                    itemService.approveItem(ap.itemId());
                    replyTo(replyChannel, MessageType.SUCCESS, "\"Da duyet san pham\"");

                    // Notify seller biáº¿t sáº£n pháº©m Ä‘Æ°á»£c duyá»‡t
                    notifySeller(sellerId, MessageType.ITEM_APPROVE,
                            "{\"itemId\":\"" + ap.itemId() + "\",\"itemName\":\"" + itemName + "\",\"status\":\"APPROVED\"}");
                }
                case ITEM_REJECT -> {
                    ItemIdPayload rp = JsonUtil.fromJson(msg.getPayload(), ItemIdPayload.class);

                    Item item = itemService.getById(rp.itemId());
                    String sellerId = item.getSellerId();
                    String itemName = item.getName();

                    itemService.rejectItem(rp.itemId());
                    replyTo(replyChannel, MessageType.SUCCESS, "\"Da tu choi san pham\"");

                    // Notify seller biáº¿t sáº£n pháº©m bá»‹ tá»« chá»‘i
                    notifySeller(sellerId, MessageType.ITEM_REJECT,
                            "{\"itemId\":\"" + rp.itemId() + "\",\"itemName\":\"" + itemName + "\",\"status\":\"REJECTED\"}");
                }
                case AUCTION_LIST -> {
                    System.out.println("ðŸ“© SERVER AUCTION_LIST from " + msg.getSenderId());
                    List<Auction> auctions = auctionService.getRunningAuctions();
                    System.out.println("ðŸ“© SERVER returning " + (auctions != null ? auctions.size() : 0) + " auctions");

                    if (auctions != null && !auctions.isEmpty()) {
                        int pageSize = 5;
                        int totalPages = (auctions.size() + pageSize - 1) / pageSize;
                        for (int page = 0; page < totalPages; page++) {
                            int start = page * pageSize;
                            int end = Math.min(start + pageSize, auctions.size());
                            replyTo(replyChannel, MessageType.AUCTION_LIST,
                                    JsonUtil.toJson(auctions.subList(start, end)));
                        }
                    } else {
                        replyTo(replyChannel, MessageType.AUCTION_LIST,
                                JsonUtil.toJson(auctions == null ? List.of() : auctions));
                    }
                }
                case AUCTION_CREATE -> {
                    Auction auction = JsonUtil.fromJson(msg.getPayload(), Auction.class);
                    if (auction.getId() == null) auction.setId(UUID.randomUUID().toString());
                    if (auction.getStartTime() == null) auction.setStartTime(LocalDateTime.now());
                    auctionService.createAuction(auction);
                    replyTo(replyChannel, MessageType.SUCCESS, "\"Phien dau gia da duoc tao\"");
                    // Broadcast cho táº¥t cáº£ client: danh sÃ¡ch phiÃªn má»›i
                    broadcastAuctionList();
                }
                case BID_HISTORY -> {
                    AuctionIdPayload ap = JsonUtil.fromJson(msg.getPayload(), AuctionIdPayload.class);
                    Auction auction = auctionService.getById(ap.auctionId());
                    List<BidTransaction> history = bidTransactionDAO.findByAuctionId(ap.auctionId());
                    JsonObject resp = new JsonObject();
                    resp.addProperty("auctionId", ap.auctionId());
                    resp.addProperty("currentPrice", auction.getCurrentPrice());
                    resp.addProperty("currentLeaderId", auction.getCurrentLeaderId());
                    resp.addProperty("currentLeaderName", auction.getCurrentLeaderName());
                    resp.addProperty("paid", auction.isPaid());
                    resp.addProperty("endTime", auction.getEndTime() == null ? null : auction.getEndTime().toString());
                    resp.add("bids", JsonParser.parseString(JsonUtil.toJson(history)));
                    replyTo(replyChannel, MessageType.BID_HISTORY, resp.toString());
                }
                case BID_PLACE -> {
                    BidRequest br = JsonUtil.fromJson(msg.getPayload(), BidRequest.class);
                    BidTransaction bid = auctionService.placeBid(br.auctionId(), br.bidderId(), br.amount());
                    bid.setBidderName(userService.getUsername(br.bidderId()));
                    // BID_UPDATE broadcast cho táº¥t cáº£ (ai Ä‘ang trong phÃ²ng Ä‘á»u tháº¥y)
                    broadcastBidUpdate(bid);
                    broadcastAuctionList();
                }
                case PAYMENT_PAY -> {
                    PaymentRequest pr = JsonUtil.fromJson(msg.getPayload(), PaymentRequest.class);
                    Auction auction = auctionService.payAuction(pr.auctionId(), pr.userId(), userService);
                    double balance = userService.getBalance(pr.userId());
                    JsonObject resp = new JsonObject();
                    resp.addProperty("auctionId", auction.getId());
                    resp.addProperty("userId", pr.userId());
                    resp.addProperty("balance", balance);
                    resp.addProperty("paid", auction.isPaid());
                    resp.addProperty("amount", auction.getCurrentPrice());
                    replyTo(replyChannel, MessageType.PAYMENT_PAY, resp.toString());
                    broadcastAuctionList();
                }
                default -> replyTo(replyChannel, MessageType.ERROR,
                        "{\"error\":\"Unknown action: " + msg.getType() + "\"}");
            }
        } catch (Exception e) {
            System.err.println("âŒ SERVER handleAction ERROR: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            String errMsg = e.getMessage() == null ? "Internal error" : e.getMessage().replace("\"", "'");
            replyTo(replyChannel, MessageType.ERROR, "{\"error\":\"" + errMsg + "\"}");
        }
    }

    @MessageMapping("/create-item")
    public void createNewItem(AuctionItem newItem) {
        AuctionItem savedItem = auctionItemService.save(newItem);
        messagingTemplate.convertAndSend("/topic/auctions", savedItem);
    }

    @MessageMapping("/get-items")
    public void sendAllItems() {
        List<AuctionItem> items = auctionItemService.getAllActiveItems();
        messagingTemplate.convertAndSend("/topic/auctions", items);
    }

    // â”€â”€ Private helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Gá»­i message riÃªng vá» Ä‘Ãºng session (khÃ´ng broadcast).
     * DÃ¹ng /queue/reply thay vÃ¬ /topic/auctions Ä‘á»ƒ trÃ¡nh "rÃ²" message sang client khÃ¡c.
     */
    private void replyTo(String sessionId, MessageType type, String payload) {
        Message response = new Message(type, "server", payload);
        String json = JsonUtil.toJson(response);
        System.out.println("ðŸ“¤ SERVER replyTo sessionId=" + sessionId + " type=" + type + " len=" + json.length());
        try {
            // Gá»­i Ä‘áº¿n queue riÃªng cá»§a session cá»¥ thá»ƒ
            messagingTemplate.convertAndSend("/queue/reply-" + sessionId, json);
            System.out.println("âœ… SERVER replyTo " + type + " sent");
        } catch (Exception e) {
            System.err.println("âŒ SERVER replyTo " + type + " ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Broadcast BID_UPDATE Ä‘áº¿n táº¥t cáº£ client Ä‘ang subscribe /topic/auctions.
     */
    private void broadcastBidUpdate(BidTransaction bid) {
        Message msg = new Message(MessageType.BID_UPDATE, "server", JsonUtil.toJson(bid));
        messagingTemplate.convertAndSend("/topic/auctions", JsonUtil.toJson(msg));
    }

    /**
     * Broadcast AUCTION_LIST Ä‘áº¿n táº¥t cáº£ client (khi cÃ³ phiÃªn má»›i hoáº·c cÃ³ bid má»›i).
     */
    private void broadcastAuctionList() {
        List<Auction> updated = auctionService.getRunningAuctions();
        Message msg = new Message(MessageType.AUCTION_LIST, "server", JsonUtil.toJson(updated));
        messagingTemplate.convertAndSend("/topic/auctions", JsonUtil.toJson(msg));
        System.out.println("ðŸ“¡ BROADCAST AUCTION_LIST â†’ " + (updated != null ? updated.size() : 0) + " auctions");
    }

    /**
     * Broadcast cho Admin biáº¿t cÃ³ sáº£n pháº©m má»›i/thay Ä‘á»•i tráº¡ng thÃ¡i cáº§n duyá»‡t.
     * Gá»­i lÃªn /topic/admin Ä‘á»ƒ Admin Ä‘ang online tháº¥y ngay.
     */
    private void broadcastAdminPendingUpdate() {
        List<Item> pending = itemService.getPendingItems();
        Message msg = new Message(MessageType.ITEM_PENDING_LIST, "server", JsonUtil.toJson(pending));
        messagingTemplate.convertAndSend("/topic/admin", JsonUtil.toJson(msg));
        System.out.println("ðŸ“¡ BROADCAST ADMIN pending â†’ " + pending.size() + " items");
    }

    /**
     * Gá»­i thÃ´ng bÃ¡o Ä‘áº¿n má»™t Seller cá»¥ thá»ƒ (khi sáº£n pháº©m Ä‘Æ°á»£c duyá»‡t/tá»« chá»‘i).
     * DÃ¹ng /queue/reply-<sessionId> cá»§a seller Ä‘Ã³.
     */
    private void notifySeller(String sellerId, MessageType type, String payload) {
        String sellerSessionId = userSessionMap.get(sellerId);
        if (sellerSessionId != null) {
            replyTo(sellerSessionId, type, payload);
            System.out.println("ðŸ“© NOTIFY seller=" + sellerId + " type=" + type);
        } else {
            System.out.println("âš ï¸ Seller " + sellerId + " khÃ´ng online, bá» qua notify");
        }
    }

    private String walletPayload(String userId, double balance) {
        JsonObject resp = new JsonObject();
        resp.addProperty("userId", userId);
        resp.addProperty("balance", balance);
        return resp.toString();
    }

    private Item parseItem(String payload) {
        JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
        String category = obj.has("category") ? obj.get("category").getAsString() : "VEHICLE";
        Class<? extends Item> clazz = switch (category.toUpperCase()) {
            case "ELECTRONICS" -> Electronics.class;
            case "ART" -> Art.class;
            default -> Vehicle.class;
        };
        return JsonUtil.fromJson(payload, clazz);
    }

    private record LoginRequest(String username, String password) {}
    private record RegisterRequest(String username, String passwordHash, String email, String role, String shopName) {}
    private record SellerPayload(String sellerId) {}
    private record ItemIdPayload(String itemId) {}
    private record BidRequest(String auctionId, String bidderId, double amount) {}
    private record AuctionIdPayload(String auctionId) {}
    private record UserIdPayload(String userId) {}
    private record WalletRequest(String userId, double amount) {}
    private record PaymentRequest(String auctionId, String userId) {}
}
