package com.blog.controller.WebSocket;



import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/notifications/{userId}")
public class NotificationWebSocket {

    private static Map<String, Set<Session>> userSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        userSessions.computeIfAbsent(userId, k -> new HashSet<>()).add(session);
        System.out.println("用户 " + userId + " 已连接");
        System.out.println("WebSocket 连接成功: " + userId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        userSessions.getOrDefault(userId, new HashSet<>()).remove(session);
        System.out.println("用户 " + userId + " 已断开");
    }

    public void sendNotification(String userId, Object notification){
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(notification);

            for (Session client : userSessions.getOrDefault(userId, new HashSet<>())) {
                client.getBasicRemote().sendText(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

