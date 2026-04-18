/**
 * Lớp Admin kế thừa User
 */
package com.auction.model;

public class Admin extends Users {

    public Admin(String id, String username, String password, String email) {
        // Mặc định vai trò là ADMIN 
        super(id, username, password, email, "ADMIN");
    }

    /**
     * Quản lý người dùng (Khóa/Mở khóa tài khoản) 
     */
    public void manageUser(String targetUserId, boolean isBlock) {
        System.out.println("Admin " + getUsername() + 
            (isBlock ? " đã khóa " : " đã mở khóa ") + "người dùng: " + targetUserId);
    }

    /**
     * Quản lý hệ thống: Hủy các phiên đấu giá vi phạm quy định 
     */
    public void cancelInvalidAuction(String auctionId) {
        System.out.println("Admin đã hủy phiên đấu giá vi phạm: " + auctionId);
    }

    /**
     * Xem thống kê báo cáo hệ thống [cite: 147]
     */
    public void viewSystemReports() {
        System.out.println("Đang hiển thị báo cáo doanh thu và lưu lượng truy cập...");
    }

    @Override
    public String getInfo() {
        return super.getInfo() + " | Quyền hạn: Toàn quyền quản trị hệ thống."; [cite: 121]
    }
}