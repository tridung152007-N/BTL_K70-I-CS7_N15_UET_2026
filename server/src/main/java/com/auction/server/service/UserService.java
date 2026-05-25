package com.auction.server.service;

import com.auction.server.dao.UserDAO;
import com.auction.server.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

public class UserService {

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public Optional<User> login(String username, String plainPassword) {
        Optional<User> userOpt = userDAO.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // === BYPASS CHO ADMIN (chỉ để test) ===
            if ("admin".equals(username)) {
                if ("123456".equals(plainPassword)) {
                    System.out.println("✅ ADMIN LOGIN BYPASS THÀNH CÔNG");
                    return userOpt;
                } else {
                    return Optional.empty();
                }
            }

            // Login bình thường cho user khác
            boolean match = BCrypt.checkpw(plainPassword, user.getPasswordHash());
            return match ? userOpt : Optional.empty();
        }
        return Optional.empty();
    }

    public void register(User user, String plainPassword) {
        try {
            if (userDAO.findByUsername(user.getUsername()).isPresent()) {
                throw new IllegalArgumentException("Username đã tồn tại");
            }

            // Hash password trước khi lưu
            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
            user.setPasswordHash(hashedPassword);

            userDAO.save(user);
            System.out.println("✅ Đăng ký thành công: " + user.getUsername());

        } catch (Exception e) {
            System.err.println("❌ Lỗi đăng ký: " + e.getMessage());
            e.printStackTrace();
        }
    }
}