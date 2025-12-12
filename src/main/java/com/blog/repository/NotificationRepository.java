package com.blog.repository;

import com.blog.entity.Notifications;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notifications,Long> {

    //获取该用户的所有通知
    List<Notifications> findByUserId(Long userId);

}
