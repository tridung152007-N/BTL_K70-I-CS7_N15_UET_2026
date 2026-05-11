package com.auction.common.network;

import java.io.Serializable;

/** DTO truyền qua Socket giữa Client và Server (JSON). */
public class Message implements Serializable {
    private MessageType type;
    private String senderId;
    private String payload;
    private long timestamp;

    public Message() {}

    public Message(MessageType type, String senderId, String payload) {
        this.type = type;
        this.senderId = senderId;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Message{type=" + type + ", sender=" + senderId + "}";
    }
}
