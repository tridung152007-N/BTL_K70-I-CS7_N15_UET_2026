package com.auction.common.network;

public enum MessageType {
    // Auth
    LOGIN, LOGOUT, REGISTER,
    // Auction
    AUCTION_LIST, AUCTION_DETAIL, AUCTION_JOIN, AUCTION_CREATE,
    BID_PLACE, BID_UPDATE, BID_HISTORY, AUCTION_END,
    // Wallet / payment
    WALLET_TOP_UP, WALLET_BALANCE, PAYMENT_PAY,
    // Item (Seller)
    ITEM_ADD, ITEM_UPDATE, ITEM_DELETE, ITEM_LIST,
    // Admin
    ITEM_PENDING_LIST, ITEM_APPROVE, ITEM_REJECT,
    // Auto-bid
    AUTO_BID_REGISTER, AUTO_BID_CANCEL,
    // System
    ERROR, SUCCESS, PING
}
