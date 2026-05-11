package com.auction.common.network;

public enum MessageType {
    // Auth
    LOGIN, LOGOUT, REGISTER,
    // Auction
    AUCTION_LIST, AUCTION_DETAIL, AUCTION_JOIN, AUCTION_CREATE,
    BID_PLACE, BID_UPDATE, AUCTION_END,
    // Item (Seller)
    ITEM_ADD, ITEM_UPDATE, ITEM_DELETE, ITEM_LIST,
    // Auto-bid
    AUTO_BID_REGISTER, AUTO_BID_CANCEL,
    // System
    ERROR, SUCCESS, PING
}
