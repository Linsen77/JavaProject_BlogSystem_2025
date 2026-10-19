package com.blog.service;

import com.blog.entity.User;
import com.blog.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.Objects;

/** Narrow identity bridge for notifications; does not replace article authentication. */
@Service
public class NotificationSessionIdentity {
    private final UserRepository users;
    public NotificationSessionIdentity(UserRepository users){this.users=users;}
    public Long userId(HttpSession session){
        try {
            Object value=session==null?null:session.getAttribute("loginUser");
            return value instanceof User u && u.getId()!=null && users.existsById(u.getId())?u.getId():null;
        } catch(IllegalStateException expired){return null;}
    }
    public Long require(HttpSession session){
        Long id=userId(session);
        if(id==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录");
        return id;
    }
    public void requireExisting(Long id){
        if(id==null || !users.existsById(id))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"登录身份失效");
    }
    public BoundUser bind(HttpSession session){return new BoundUser(require(session),session,this);}
    public static final class BoundUser implements Principal {
        private final Long id;
        private final HttpSession session;
        private final NotificationSessionIdentity identity;
        private BoundUser(Long id,HttpSession session,NotificationSessionIdentity identity){this.id=id;this.session=session;this.identity=identity;}
        public String getName(){return id.toString();}
        public boolean valid(){return Objects.equals(id,identity.userId(session));}
    }
}
