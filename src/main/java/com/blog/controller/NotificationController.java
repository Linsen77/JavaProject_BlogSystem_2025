package com.blog.controller;


import com.blog.entity.Notifications;
import com.blog.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.management.Notification;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    //获取用户的所有通知
    @GetMapping("/{userId}")
    public List<Notifications> getNotifications(@RequestParam Long userId) {
        return notificationService.getNotifications(userId);
    }

    //标记通知为已读
    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markNotificationRead(id);
    }
}
