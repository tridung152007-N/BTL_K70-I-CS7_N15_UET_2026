package com.auction.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuctionSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionSystemApplication.class, args);

        // Get actual IP address of the machine
        String serverIp = getServerIp();
        String serverPort = "8080";
        String wsEndpoint = "/ws-auction";

        System.out.println("=====================================");
        System.out.println("🚀 AUCTION SERVER ĐÃ KHỞI ĐỘNG!");
        System.out.println("Server IP: " + serverIp);
        System.out.println("Server Port: " + serverPort);
        System.out.println("WebSocket Endpoint: ws://" + serverIp + ":" + serverPort + wsEndpoint);
        System.out.println("=====================================");
    }

    /**
     * Get the actual IP address of this machine (not localhost)
     */
    private static String getServerIp() {
        try {
            // Try to get the non-loopback IPv4 address
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .flatMap(ni -> Collections.list(ni.getInetAddresses()).stream())
                    .filter(ia -> !ia.isLoopbackAddress() && ia.getHostAddress().contains("."))
                    .map(InetAddress::getHostAddress)
                    .findFirst()
                    .orElse("localhost");
        } catch (Exception e) {
            System.err.println("⚠️ Could not get server IP: " + e.getMessage());
            return "localhost";
        }
    }
}