# 📱 Hệ Thống Đấu Giá Trực Tuyến - Giao Diện JavaFX

Đây là bộ giao diện **hoàn chỉnh** cho ứng dụng đấu giá trực tuyến được xây dựng bằng **JavaFX** với thiết kế hiện đại, responsive và dễ sử dụng.

## 🎨 Các Màn Hình Được Cung Cấp

### 1. **LoginScreen** - Màn hình đăng nhập
- Thiết kế gradient hiện đại
- Hỗ trợ 3 vai trò: Bidder (Người mua), Seller (Người bán), Admin
- Tùy chọn "Ghi nhớ đăng nhập"
- Thông báo lỗi rõ ràng
- Chuyển hướng tới màn hình phù hợp theo vai trò

### 2. **MainScreen** - Danh sách phiên đấu giá
- Thanh điều hướng với menu chính
- Bộ lọc theo danh mục và sắp xếp
- Grid card responsive hiển thị sản phẩm
- Mỗi card hiển thị:
    - Hình ảnh sản phẩm
    - Tên sản phẩm
    - Danh mục
    - Giá hiện tại và giá khởi điểm
    - Số lượt bid
    - Thời gian còn lại

### 3. **AuctionDetailScreen** - Chi tiết phiên đấu giá
- Hiển thị hình ảnh sản phẩm (chính + các hình phụ)
- Thông tin chi tiết sản phẩm
- **Countdown timer** realtime (cập nhật hàng giây)
- Hiển thị giá cao nhất hiện tại
- **Form đặt giá** với validation
- Bật/tắt **Auto-bid** (đấu giá tự động)
- **Lịch sử đặt giá** dạng bảng
- Phía bên dưới: Mô tả sản phẩm chi tiết

### 4. **SellerDashboard** - Bàn điều khiển người bán
- Thống kê tổng quan (4 card):
    - Tổng phiên đấu giá
    - Số phiên đang diễn hành
    - Tổng doanh thu
    - Tỷ lệ thành công
- Menu sidebar với các filter
- Nút tạo phiên mới & import CSV
- Bảng danh sách phiên với các hành động (Edit, Delete, View Stats)
