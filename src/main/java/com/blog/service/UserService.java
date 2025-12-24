package com.blog.service;

import com.blog.entity.User;

public interface UserService{

    public boolean existsByEmail(String email);

    public User findByEmail(String email);

    public boolean save(User user);
}
