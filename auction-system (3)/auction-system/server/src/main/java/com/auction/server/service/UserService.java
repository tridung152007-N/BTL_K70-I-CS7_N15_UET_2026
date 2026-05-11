package com.auction.server.service;

import com.auction.common.exception.UserNotFoundException;
import com.auction.server.dao.UserDAO;
import com.auction.server.model.User;

import java.util.Optional;

public class UserService {
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) { this.userDAO = userDAO; }

    public Optional<User> login(String username, String passwordHash) {
        return userDAO.findByUsername(username)
                .filter(u -> u.getPasswordHash().equals(passwordHash));
    }

    public void register(User user) {
        userDAO.findByUsername(user.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("Username đã tồn tại: " + u.getUsername());
        });
        userDAO.save(user);
    }

    public User getById(String id) {
        return userDAO.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }
}
