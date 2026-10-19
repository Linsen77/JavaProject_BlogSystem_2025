package com.blog.config;

import com.blog.service.NotificationSessionIdentity;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

/** Runs after the existing security wrapper and before Tomcat's WebSocket upgrade. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE-100)
public class NotificationHandshakeFilter extends OncePerRequestFilter {
    private final NotificationSessionIdentity identity;
    private final Set<String> origins;
    public NotificationHandshakeFilter(NotificationSessionIdentity identity,
            @Value("${blog.websocket.allowed-origins:http://localhost:5173}") String allowed){
        this.identity=identity;this.origins=new HashSet<>();
        for(String origin:allowed.split(",")){
            String value=origin.trim();
            if(value.isEmpty() || value.contains("*") || !(value.startsWith("http://") || value.startsWith("https://")))
                throw new IllegalArgumentException("Explicit WebSocket origins required");
            origins.add(value);
        }
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request){
        return !request.getRequestURI().substring(request.getContextPath().length()).startsWith("/notifications/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
        if(!origins.contains(request.getHeader("Origin"))){response.setStatus(403);return;}
        HttpSession session=request.getSession(false);
        Long user=identity.userId(session);
        if(user==null){response.setStatus(401);return;}
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if(!path.equals("/notifications/"+user)){response.setStatus(404);return;}
        Principal verified=identity.bind(session);
        // Tomcat transfers this request Principal into jakarta.websocket.Session.
        chain.doFilter(new HttpServletRequestWrapper(request){
            @Override public Principal getUserPrincipal(){return verified;}
            @Override public String getRemoteUser(){return verified.getName();}
        },response);
    }
}
