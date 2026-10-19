package com.blog.controller.WebSocket;

import com.blog.entity.Notifications;
import com.blog.service.NotificationSessionIdentity.BoundUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/notifications/{userId}")
public class NotificationWebSocket {
    private static final Logger log=LoggerFactory.getLogger(NotificationWebSocket.class);
    private static final String IDENTITY=NotificationWebSocket.class.getName()+".identity";
    private static final Map<String,Set<Session>> userSessions=new ConcurrentHashMap<>();
    // Spring-owned instance sends messages; Tomcat-owned instances handle callbacks.
    @Autowired private ObjectMapper mapper;
    @OnOpen public void onOpen(Session session,@PathParam("userId") String pathUser){
        if(!(session.getUserPrincipal() instanceof BoundUser user) || !user.valid() || !user.getName().equals(pathUser)){
            close(session,CloseReason.CloseCodes.VIOLATED_POLICY);return;
        }
        session.getUserProperties().put(IDENTITY,user);
        userSessions.compute(user.getName(),(id,set)->{
            if(set==null)set=ConcurrentHashMap.newKeySet();set.add(session);return set;
        });
    }
    @OnClose public void onClose(Session session){remove(session);}
    @OnError public void onError(Session session,Throwable error){close(session,CloseReason.CloseCodes.UNEXPECTED_CONDITION);}
    @OnMessage public void onMessage(Session session,String message){
        // Push-only endpoint: no JSON commands or client-selected recipient routing.
        close(session,CloseReason.CloseCodes.VIOLATED_POLICY);
    }
    private static void remove(Session session){
        Object value=session.getUserProperties().get(IDENTITY);
        if(value instanceof BoundUser user)userSessions.computeIfPresent(user.getName(),(id,set)->{
            set.remove(session);return set.isEmpty()?null:set;
        });
    }
    private static void close(Session session,CloseReason.CloseCode code){
        remove(session);
        try{if(session.isOpen())session.close(new CloseReason(code,"Notification session closed"));}
        catch(Exception ignored){log.debug("Notification connection cleanup failed");}
    }
    public void sendNotification(String userId,Notifications notification){
        if(notification==null || notification.getUser()==null || notification.getUser().getId()==null
                || !notification.getUser().getId().toString().equals(userId)){
            log.warn("Refused mismatched notification recipient");return;
        }
        final String json;
        try{json=mapper.writeValueAsString(notification);}
        catch(Exception error){log.warn("Notification serialization failed");return;}
        for(Session client:userSessions.getOrDefault(userId,Set.of())){
            try{
                synchronized(client){
                    Object value=client.getUserProperties().get(IDENTITY);
                    if(!(value instanceof BoundUser user) || !user.valid() || !user.getName().equals(userId)){
                        close(client,CloseReason.CloseCodes.VIOLATED_POLICY);continue;
                    }
                    if(client.isOpen())client.getBasicRemote().sendText(json);else remove(client);
                }
            }catch(Exception error){close(client,CloseReason.CloseCodes.UNEXPECTED_CONDITION);log.warn("Notification send failed; connection removed");}
        }
    }
}
