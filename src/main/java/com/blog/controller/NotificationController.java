package com.blog.controller;


import com.blog.entity.Notifications;
import com.blog.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.management.Notification;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import com.blog.service.NotificationSessionIdentity;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    @Autowired private NotificationSessionIdentity identity;

    //获取用户的所有通知
    @GetMapping("/{userId}")
    public List<Notifications> getNotifications(@PathVariable Long userId,HttpSession session) {
        return notificationService.getNotifications(userId,identity.require(session));
    }

    //标记通知为已读
    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id,HttpSession session) {
        notificationService.markNotificationRead(id,identity.require(session));
    }
}
