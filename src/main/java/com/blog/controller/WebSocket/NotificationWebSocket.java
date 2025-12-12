package com.blog.controller.WebSocket;


import com.blog.entity.Notifications;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
@ServerEndpoint("/notifications")
public class NotificationWebSocket {

    private static Set<Session> clients = new HashSet<>();

    @OnOpen
    public void onOpen(Session session) {
        clients.add(session);//新连接建立时加入
    }

    @OnClose
    public void onClose(Session session) {
        clients.remove(session);//连接关闭时移除
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        //处理收到的消息，可以向客户端返回数据
    }

    public void sendNotification(String message){
        for(Session client : clients){
            try{
                client.getBasicRemote().sendText(message);//推送消息
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
