# Auction System – Hệ thống đấu giá trực tuyến

Hệ thống đấu giá trực tuyến theo thời gian thực, cho phép nhiều người dùng từ nhiều máy tính khác nhau cùng tham gia đấu giá sản phẩm. Người bán đăng sản phẩm, quản trị viên duyệt, người mua vào phòng đấu giá và đặt giá cạnh tranh. Kết quả cập nhật tức thì đến tất cả người dùng đang online.

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|------------|-----------|
| Ngôn ngữ | Java 21 |
| Build tool | Maven 3.9+ |
| Server framework | Spring Boot 3.3.4 |
| Giao tiếp real-time | WebSocket + STOMP |
| Giao diện client | JavaFX 21.0.3 |
| Cơ sở dữ liệu | MySQL 8+ |
| Serialization | Gson 2.11.0 |
| Mã hoá mật khẩu | jBCrypt 0.4 |
| Quản lý phụ thuộc | Lombok 1.18.46 |

## Yêu cầu cài đặt

- **JDK 21+** — [tải tại đây](https://adoptium.net/)
- **Maven 3.9+** — [tải tại đây](https://maven.apache.org/download.cgi)
- **MySQL 8+** — [tải tại đây](https://dev.mysql.com/downloads/)

Kiểm tra đã cài đúng chưa:
```bash
java -version    # cần >= 21
mvn -version     # cần >= 3.9
mysql --version  # cần >= 8.0
```

## Cấu trúc module

```
auction-system/
├── common/          # DTO, Exception, MessageType, JsonUtil dùng chung
├── server/          # Spring Boot server – WebSocket, business logic, DAO, MySQL
├── client/          # JavaFX client – giao diện người dùng, kết nối STOMP
├── migrate_add_status.sql       # Migration thêm cột status cho bảng items
├── migrate_wallet_payment.sql   # Migration thêm bảng ví và thanh toán
└── pom.xml
```

## Cài đặt database

```sql
-- Tạo database
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Chạy migration
USE auction_db;
SOURCE migrate_add_status.sql;
SOURCE migrate_wallet_payment.sql;
```

Cấu hình kết nối MySQL trong `server/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC
    username: root
    password: 12345   # đổi thành password MySQL của bạn
```

## Cấu hình kết nối LAN

Nếu client chạy trên máy khác (kết nối qua mạng LAN), sửa file `client/src/main/resources/config.properties`:
```properties
server.host=192.168.x.x   # IP của máy chạy server
server.port=8080
server.ws.endpoint=/ws-auction
```

Nếu chạy cùng một máy, giữ nguyên `server.host=localhost`.

## Build

```bash
# Tất cả hệ điều hành (Windows / Linux / macOS)
mvn clean install -Dmaven.test.skip=true
```

> Thêm `-Dmaven.test.skip=true` để tránh lỗi khi JDK version cao hơn 21.

## Chạy chương trình

**Bước 1 — Chạy Server trước** (máy chủ):

```bash
# Windows / Linux / macOS
mvn spring-boot:run -pl server
```

Chờ đến khi thấy log:
```
🚀 AUCTION SERVER ĐÃ KHỞI ĐỘNG!
Server IP: 192.168.x.x
Server Port: 8080
```

**Bước 2 — Chạy Client** (mỗi máy người dùng mở một terminal riêng):

```bash
# Windows / Linux / macOS
mvn javafx:run -pl client
```

> Server phải chạy trước. Client chạy sau. Nhiều máy có thể chạy client cùng lúc.

## Chức năng đã hoàn thành

**Xác thực người dùng**
- [x] Đăng ký tài khoản (vai trò Bidder hoặc Seller)
- [x] Đăng nhập / Đăng xuất
- [x] Mã hoá mật khẩu bằng BCrypt

**Quản lý sản phẩm (Seller)**
- [x] Thêm sản phẩm (3 loại: Xe cộ, Nghệ thuật, Điện tử)
- [x] Sửa / Xoá sản phẩm
- [x] Theo dõi trạng thái duyệt (Chờ duyệt / Đã duyệt / Từ chối)
- [x] Nhận thông báo real-time khi sản phẩm được duyệt hoặc bị từ chối
- [x] Tạo phiên đấu giá từ sản phẩm đã được duyệt

**Quản trị viên (Admin)**
- [x] Xem danh sách sản phẩm chờ duyệt
- [x] Duyệt hoặc từ chối sản phẩm
- [x] Nhận cập nhật real-time khi có sản phẩm mới gửi lên

**Đấu giá (Bidder)**
- [x] Xem danh sách phiên đấu giá đang diễn ra
- [x] Vào phòng đấu giá
- [x] Đặt giá thủ công
- [x] Đặt giá tự động (Auto-bid) theo giới hạn tối đa và bước tăng
- [x] Xem lịch sử đặt giá trong phòng
- [x] Biểu đồ lịch sử giá theo thời gian
- [x] Đồng hồ đếm ngược thời gian còn lại
- [x] Cập nhật giá real-time đến tất cả người dùng trong phòng
- [x] Tự động kết thúc phiên khi hết giờ

**Ví tiền & Thanh toán**
- [x] Xem số dư ví
- [x] Nạp tiền vào ví
- [x] Thanh toán sau khi thắng đấu giá

**Đồng bộ real-time (nhiều máy)**
- [x] Khi một máy đặt giá, tất cả máy khác thấy giá mới ngay lập tức
- [x] Khi một máy tạo phiên đấu giá mới, tất cả máy khác thấy trong danh sách ngay
- [x] Khi Seller thêm sản phẩm, Admin online thấy thông báo ngay

## Báo cáo & Demo

- 📄 **Báo cáo PDF:  https://drive.google.com/file/d/1xfxpS3oNwmX3OxWsI3hijHOUOCSzoyBx/view?usp=sharing
