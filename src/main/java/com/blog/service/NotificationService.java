package com.blog.service;

import com.blog.controller.WebSocket.NotificationWebSocket;
import com.blog.entity.Notifications;
import com.blog.entity.User;
import com.blog.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.Notification;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationWebSocket notificationWebSocket;

    //发送通知到作者
    public void sendNotificationToAuthor(User author, String content) {
        Notifications notification = new Notifications();
        notification.setContent(content);
        notification.setUser(author);
        notification.setRead(false);//默认未读
        notification.setCreatetime(LocalDateTime.now());

        notificationRepository.save(notification);

        //通过WebSocket推送
        notificationWebSocket.sendNotification("新通知： " + content);

    }

    //获取该用户的所有通知
    public List<Notifications> getNotifications(Long userId){
        return notificationRepository.findByUserId(userId);
    }

    //标记通知为已读
    public void markNotificationRead(Long notificationId){
        Notifications notifications = notificationRepository.findById(notificationId).orElseThrow(() -> new RuntimeException("通知不存在"));
        notifications.setRead(true);
        notificationRepository.save(notifications);
    }

}
