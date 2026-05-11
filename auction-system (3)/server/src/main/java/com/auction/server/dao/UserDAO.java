package com.auction.server.dao;

import com.auction.server.model.User;
import java.util.List;
import java.util.Optional;

public interface UserDAO {
    void save(User user);
    Optional<User> findById(String id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    void update(User user);
    void delete(String id);
}
