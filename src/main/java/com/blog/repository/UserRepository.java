package com.blog.repository;

import com.blog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {
    //根据email寻找用户
    User findByEmail(String email);

    //判断邮箱是否注册
    boolean existsByEmail(String email);
}