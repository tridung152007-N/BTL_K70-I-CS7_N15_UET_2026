package com.auction.model;

/**
 * Lớp Admin kế thừa Users
 */
public class Admin extends Users {

    public Admin(String id, String username, String password, String email) {
        super(id, username, password, email, "ADMIN");
    }

    public void manageUser(String targetUserId, boolean isBlock) {
        System.out.println("Admin " + getUsername() +
            (isBlock ? " đã khóa " : " đã mở khóa ") + "người dùng: " + targetUserId);
    }

    public void cancelInvalidAuction(String auctionId) {
        System.out.println("Admin đã hủy phiên đấu giá vi phạm: " + auctionId);
    }

    public void viewSystemReports() {
        System.out.println("Đang hiển thị báo cáo doanh thu và lưu lượng truy cập...");
    }

    @Override
    public String getInfo() {
        return super.getInfo() + " | Quyền hạn: Toàn quyền quản trị hệ thống.";
    }
}