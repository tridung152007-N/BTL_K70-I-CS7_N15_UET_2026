# Auction System – Hệ thống đấu giá trực tuyến

> Bài tập lớn môn Lập trình nâng cao – Java

## Cấu trúc module

| Module   | Mô tả |
|----------|-------|
| `common` | DTO, Exception, MessageType, JsonUtil dùng chung giữa server và client |
| `server` | Spring Boot WebSocket server – business logic, DAO (MySQL), AuctionTimer |
| `client` | JavaFX GUI – Controller, SocketClient (STOMP), các màn hình đấu giá |

## Yêu cầu

- Java 21+
- Maven 3.9+
- MySQL 8+ (database `auction_db`)

## Cấu hình

### Server (`server/src/main/resources/application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC
    username: root
    password: 12345
server:
  port: 8080
```

### Client (`client/src/main/resources/config.properties`)

```properties
server.host=localhost
server.port=8080
server.ws.endpoint=/ws-auction
```

> **Kết nối LAN:** đổi `server.host` thành IP máy chạy server, ví dụ `server.host=192.168.1.10`

## Build & Chạy

```bash
# Build toàn bộ (bỏ qua test để tránh lỗi JDK version mismatch)
mvn clean install -Dmaven.test.skip=true

# Chạy Server
mvn spring-boot:run -pl server

# Chạy Client (terminal khác)
mvn javafx:run -pl client
```

> **Lưu ý JDK:** Project được cấu hình cho Java 21. Nếu Maven chạy với JDK cao hơn (ví dụ JDK 25), hãy thêm `-Dmaven.test.skip=true` khi build để tránh lỗi compile test.

## Tính năng

| Nhóm | Chức năng |
|------|-----------|
| **Xác thực** | Đăng ký, đăng nhập, đăng xuất (Bidder / Seller / Admin) |
| **Đấu giá** | Xem danh sách, vào phòng đấu giá real-time qua WebSocket/STOMP |
| **Đặt giá** | Đặt giá thủ công, auto-bid (đặt giá tự động theo giới hạn) |
| **Seller** | Thêm / sửa / xóa item, tạo phiên đấu giá |
| **Admin** | Duyệt / từ chối item, quản lý hệ thống |
| **Ví tiền** | Nạp tiền, xem số dư, thanh toán sau khi thắng |
| **Lịch sử** | Xem lịch sử đặt giá của từng phiên |

## Design Patterns

| Pattern | Áp dụng |
|---------|---------|
| **Singleton** | `SocketClient` – một kết nối STOMP duy nhất cho toàn client |
| **Factory Method** | `ItemFactory` – tạo các loại item (Art, Electronics, Vehicle) |
| **Observer** | `AuctionService` → `AuctionObserver` → broadcast tới client |
| **DAO** | `AuctionDAO`, `UserDAO`, `ItemDAO`, `BidTransactionDAO` tách biệt persistence |

## Giao tiếp Client ↔ Server

Client gửi `Message` JSON qua STOMP destination `/app/action`.  
Server trả kết quả về `/topic/auctions`, `/topic/admin`, hoặc `/queue/reply-{clientId}`.

```
MessageType: LOGIN, REGISTER, AUCTION_LIST, AUCTION_CREATE,
             BID_PLACE, BID_HISTORY, AUCTION_END,
             WALLET_TOP_UP, WALLET_BALANCE, PAYMENT_PAY,
             ITEM_ADD/UPDATE/DELETE, ITEM_APPROVE/REJECT,
             AUTO_BID_REGISTER/CANCEL, ERROR, SUCCESS
```

## Database

Chạy migration trước khi khởi động server lần đầu:

```sql
-- Thêm cột status cho item
source migrate_add_status.sql

-- Thêm bảng ví và thanh toán
source migrate_wallet_payment.sql
```

## Conventional Commits

```
feat: thêm tính năng mới
fix:  sửa lỗi
test: thêm/sửa test
docs: cập nhật tài liệu
refactor: tái cấu trúc code
```
Link báo cáo :file:///C:/Users/admin/Downloads/BaoCao_DauGia_BTL_CS7_N15%20(1).pdf