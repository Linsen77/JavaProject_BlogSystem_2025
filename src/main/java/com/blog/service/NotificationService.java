package com.blog.service;

import com.blog.controller.WebSocket.NotificationWebSocket;
import com.blog.entity.Notifications;
import com.blog.entity.User;
import com.blog.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationService {
    private static final Logger log=LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationWebSocket notificationWebSocket;
    @Autowired private NotificationSessionIdentity identity;

    //发送通知到作者
    @Transactional
    public void sendNotificationToAuthor(User author, String content) {
        Notifications notification = new Notifications();
        notification.setContent(content);
        notification.setUser(author);
        notification.setReadFlag(false);//默认未读
        notification.setCreatetime(LocalDateTime.now());

        notificationRepository.save(notification);

        // REQUIRED joins the interaction transaction (or starts one for standalone calls).
        // Rolled-back work never publishes; online push remains best-effort after commit.
        String recipient=author.getId().toString();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCommit(){
                try{notificationWebSocket.sendNotification(recipient,notification);}
                catch(RuntimeException failure){log.warn("Notification push failed after commit; history retained");}
            }
        });

    }

    //获取该用户的所有通知
    public List<Notifications> getNotifications(Long userId,Long actorId){
        identity.requireExisting(actorId);
        if(!actorId.equals(userId))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"通知不存在");
        return notificationRepository.findByUserId(actorId);
    }

    //标记通知为已读
    @Transactional
    public void markNotificationRead(Long notificationId,Long actorId){
        identity.requireExisting(actorId);
        Notifications notifications = notificationRepository.findByIdAndUserId(notificationId,actorId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"通知不存在"));
        notifications.setReadFlag(true);
        notificationRepository.save(notifications);
    }

}
