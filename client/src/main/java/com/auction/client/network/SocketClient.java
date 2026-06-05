package com.auction.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.auction.common.network.Message;
import com.auction.common.util.JsonUtil;

import jakarta.websocket.ContainerProvider;

public class SocketClient {

    private static SocketClient instance;
    private StompSession stompSession;
    private final ThreadPoolTaskScheduler taskScheduler;

    private volatile String stompSessionId;
    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();
    private final String replyChannelId = UUID.randomUUID().toString();

    private final String serverIp;
    private final int serverPort;
    private final String wsEndpoint;

    private SocketClient() {
        Properties props = new Properties();
        try (InputStream in = SocketClient.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            System.err.println("Không đọc được config.properties: " + e.getMessage());
        }
        serverIp   = props.getProperty("server.host",        "localhost");
        serverPort = Integer.parseInt(props.getProperty("server.port", "8080"));
        wsEndpoint = props.getProperty("server.ws.endpoint", "/ws-auction");
        System.out.println("Server config: " + serverIp + ":" + serverPort + wsEndpoint);

        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(2);
        taskScheduler.setThreadNamePrefix("stomp-task-");
        taskScheduler.initialize();
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null) instance = new SocketClient();
        return instance;
    }

    public synchronized void connect() throws ExecutionException, InterruptedException {
        if (stompSession != null && stompSession.isConnected()) return;

        String wsUrl = "ws://" + serverIp + ":" + serverPort + wsEndpoint;
        System.out.println("STOMP connecting to " + wsUrl + "...");

        try {
            jakarta.websocket.WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.setDefaultMaxTextMessageBufferSize(256 * 1024);
            container.setDefaultMaxBinaryMessageBufferSize(256 * 1024);
        } catch (Exception e) {
            System.err.println("Không thể cấu hình WebSocket buffer: " + e.getMessage());
        }

        StandardWebSocketClient wsClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(wsClient);
        stompClient.setMessageConverter(new StringMessageConverter());
        stompClient.setTaskScheduler(taskScheduler);

        stompSession = stompClient.connectAsync(
                wsUrl,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        // Láº¥y session-id tá»« CONNECTED frame header cá»§a server
                        // ÄÃ¢y lÃ  ID mÃ  Spring server dÃ¹ng trong headerAccessor.getSessionId()
                        String serverSessionId = connectedHeaders.getFirst("session");
                        if (serverSessionId != null && !serverSessionId.isEmpty()) {
                            stompSessionId = serverSessionId;
                            System.out.println(" STOMP connected, server sessionId=" + stompSessionId);
                        } else {
                            // Fallback: dÃ¹ng session ID tá»« STOMP client
                            stompSessionId = session.getSessionId();
                            System.out.println(" STOMP connected, client sessionId=" + stompSessionId);
                        }

                        session.subscribe("/topic/auctions", makeFrameHandler());
                        session.subscribe("/topic/admin", makeFrameHandler());
                        session.subscribe("/queue/reply-" + replyChannelId, makeFrameHandler());

                        System.out.println("âœ… Subscribed to /queue/reply-" + replyChannelId);
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable ex) {
                        System.err.println(" STOMP transport error: " + (ex != null ? ex.getMessage() : "null"));
                        if (stompSession == session) stompSession = null;
                    }

                    @Override
                    public void handleException(StompSession session, StompCommand command,
                                                StompHeaders headers, byte[] payload, Throwable ex) {
                        System.err.println("STOMP exception: command=" + command
                                + (ex != null ? " " + ex.getMessage() : ""));
                    }
                }).get();
    }

    private StompFrameHandler makeFrameHandler() {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                try {
                    String raw = (String) payload;
                    Message msg = JsonUtil.fromJson(raw, Message.class);
                    System.out.println("RECEIVED type=" + msg.getType()
                            + " from=" + msg.getSenderId()
                            + " dest=" + headers.getDestination());

                    for (MessageListener l : listeners) {
                        l.onMessageReceived(msg);
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi parse message: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
    }

    public synchronized boolean ensureConnected() {
        if (stompSession != null && stompSession.isConnected()) return true;
        try {
            connect();
            return stompSession != null && stompSession.isConnected();
        } catch (Exception ex) {
            System.err.println("Không thể kết nối STOMP: " + ex.getMessage());
            try {
                Thread.sleep(500);
                connect();
                return stompSession != null && stompSession.isConnected();
            } catch (Exception retryEx) {
                System.err.println("Thử lại kết nối STOMP thất bại: " + retryEx.getMessage());
                return false;
            }
        }
    }

    public boolean send(String destination, Message message) {
        if (!ensureConnected()) {
            System.err.println("Chưa kết nối server, không thể gửi: " + message.getType());
            return false;
        }
        String json = JsonUtil.toJson(message);
        System.out.println("SEND destination=" + destination + " type=" + message.getType() + " len=" + json.length());
        StompHeaders headers = new StompHeaders();
        headers.setDestination(destination);
        headers.add("reply-to", replyChannelId);
        stompSession.send(headers, json);
        return true;
    }

    public boolean send(Message message) {
        return send("/app/action", message);
    }

    public void addListener(MessageListener listener) {
        listeners.add(listener);
        System.out.println(" Listener added, count=" + listeners.size());
    }

    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
        System.out.println(" Listener removed, count=" + listeners.size());
    }

    public String getStompSessionId() {
        return stompSessionId;
    }

    public void disconnect() {
        try {
            if (stompSession != null && stompSession.isConnected()) {
                stompSession.disconnect();
            }
        } finally {
            stompSession = null;
            stompSessionId = null;
        }
    }
}
