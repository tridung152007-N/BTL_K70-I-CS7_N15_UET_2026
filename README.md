# Auction System – Hệ thống đấu giá trực tuyến

> Bài tập lớn môn Lập trình nâng cao – Java

## Cấu trúc module

| Module   | Mô tả |
|----------|-------|
| `common` | DTO, Exception, Util dùng chung |
| `server` | Business logic, DAO, Socket server |
| `client` | JavaFX GUI, Controller, Socket client |

## Yêu cầu

- Java 21+
- Maven 3.9+

## Chạy nhanh

```bash
# Build tất cả
mvn clean install

# Chạy Server
mvn exec:java -pl server -Dexec.mainClass="com.auction.server.ServerApp"

# Chạy Client (terminal khác)
mvn javafx:run -pl client
```

## Design Patterns áp dụng

- **Singleton**: `SocketServer`, `SocketClient`
- **Factory Method**: `ItemFactory`
- **Observer**: `AuctionService` → `ClientHandler`
- **Strategy** (mở rộng): xử lý các loại bid

## Conventional Commits

```
feat: thêm auto-bidding
fix: sửa race condition trong AuctionService
test: thêm ConcurrentBidTest
docs: cập nhật README
```
