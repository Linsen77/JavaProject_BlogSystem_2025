package com.blog.service;

import com.blog.entity.User;
import com.blog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public interface UserService{

    public boolean existsByEmail(String email);

    public User findByEmail(String email);

    public boolean save(User user);
}
