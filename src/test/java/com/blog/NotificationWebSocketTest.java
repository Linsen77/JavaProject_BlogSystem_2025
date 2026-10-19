package com.blog;

import com.blog.entity.*;
import com.blog.repository.*;
import com.blog.service.NotificationService;
import com.blog.util.PasswordUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.config.location=classpath:application-ownership-test.yml","spring.mail.username=synthetic@example.invalid",
        "server.address=127.0.0.1","server.servlet.session.cookie.secure=false"})
@Import(NotificationWebSocketTest.TestSessions.class)
class NotificationWebSocketTest {
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){ArticleOwnershipTest.isolatedDatabase(r);}
    @LocalServerPort int port;
    @MockBean JavaMailSender mail;
    @Autowired UserRepository users;
    @Autowired NotificationRepository notices;
    @Autowired NotificationService service;
    @Autowired ObjectMapper json;
    @Autowired com.blog.controller.WebSocket.NotificationWebSocket endpoint;
    @Autowired com.blog.service.ArticleService articleService;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    User a,b;List<WebSocket> sockets=new ArrayList<>();
    static final String ORIGIN="http://localhost:5173";
    @TestConfiguration static class TestSessions {
        @Bean ExpireSession expireSession(){return new ExpireSession();}
    }
    @RestController static class ExpireSession {
        @PostMapping("/api/auth/test-expire-session") public void expire(HttpSession session){session.invalidate();}
    }
    static class Inbox implements WebSocket.Listener {
        BlockingQueue<String> messages=new LinkedBlockingQueue<>();CompletableFuture<Integer> closed=new CompletableFuture<>();StringBuilder buffer=new StringBuilder();
        public void onOpen(WebSocket ws){ws.request(1);}
        public CompletionStage<?> onText(WebSocket ws,CharSequence text,boolean last){buffer.append(text);if(last){messages.add(buffer.toString());buffer.setLength(0);}ws.request(1);return null;}
        public CompletionStage<?> onClose(WebSocket ws,int code,String reason){closed.complete(code);return null;}
        public void onError(WebSocket ws,Throwable error){closed.completeExceptionally(error);}
        String take() throws Exception {String text=messages.poll(3,TimeUnit.SECONDS);assertThat(text).as("real WebSocket message").isNotNull();return text;}
    }
    @BeforeEach void seed(){a=user("ws-a");b=user("ws-b");}
    User user(String name){User u=new User();u.setName(name);u.setEmail(name+UUID.randomUUID()+"@example.invalid");u.setPassword(PasswordUtils.encryptPassword("synthetic-password"));return users.saveAndFlush(u);}
    HttpClient client(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(java.time.Duration.ofSeconds(3)).build();}
    HttpClient login(User u) throws Exception {HttpClient c=client();login(c,u);return c;}
    void login(HttpClient c,User u) throws Exception {
        var response=c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/auth/login"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("email",u.getEmail(),"password","synthetic-password")))).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);assertThat(json.readTree(response.body()).get("code").asInt()).isEqualTo(200);
        assertThat(((CookieManager)c.cookieHandler().orElseThrow()).getCookieStore().getCookies()).anyMatch(cookie->cookie.getName().equals("JSESSIONID"));
    }
    WebSocket connect(HttpClient c,Long target,String origin,Inbox inbox) throws Exception {
        int before=registered(target);
        var builder=c.newWebSocketBuilder().connectTimeout(java.time.Duration.ofSeconds(3));if(origin!=null)builder.header("Origin",origin);
        WebSocket ws=builder.buildAsync(URI.create("ws://127.0.0.1:"+port+"/notifications/"+target),inbox).get(4,TimeUnit.SECONDS);sockets.add(ws);
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
        while(registered(target)<=before && System.nanoTime()<deadline)Thread.sleep(10);
        assertThat(registered(target)).isGreaterThan(before);return ws;
    }
    int registered(Long target) throws Exception {
        // Observe asynchronous server OnOpen readiness, not a substitute for network handshake.
        var field=com.blog.controller.WebSocket.NotificationWebSocket.class.getDeclaredField("userSessions");field.setAccessible(true);
        var map=(Map<?,?>)field.get(null);Object value=map.get(target.toString());return value instanceof Set<?> set?set.size():0;
    }
    void rejected(HttpClient c,Long target,String origin,int expected) throws Exception {
        try {connect(c,target,origin,new Inbox()).abort();Assertions.fail("Unauthenticated/forged upgrade was accepted");}
        catch(ExecutionException e){assertThat(e.getCause()).isInstanceOf(WebSocketHandshakeException.class);assertThat(((WebSocketHandshakeException)e.getCause()).getResponse().statusCode()).isEqualTo(expected);}
    }
    @AfterEach void cleanup() {
        sockets.forEach(WebSocket::abort);
        for(User u:List.of(a,b)){notices.findByUserId(u.getId()).forEach(n->notices.deleteById(n.getId()));if(users.existsById(u.getId()))users.deleteById(u.getId());}
        org.mockito.Mockito.verifyNoInteractions(mail);
    }
    @Test void cookieLoginAndTimeMessage() throws Exception {
        Inbox box=new Inbox();connect(login(a),a.getId(),ORIGIN,box);service.sendNotificationToAuthor(a,"synthetic-online");
        var message=json.readTree(box.take());assertThat(message.get("content").asText()).isEqualTo("synthetic-online");
        assertThat(message.get("user").get("id").asLong()).isEqualTo(a.getId());assertThat(message.get("createtime").asText()).contains("T");
        assertThat(notices.findByUserId(a.getId())).hasSize(1);
    }
    @Test void anonymousRejected() throws Exception {rejected(client(),a.getId(),ORIGIN,401);}
    @Test void forgedPathRejected() throws Exception {rejected(login(a),b.getId(),ORIGIN,404);}
    @Test void badOriginRejected() throws Exception {rejected(login(a),a.getId(),"https://untrusted.example",403);}
    @Test void missingOriginRejected() throws Exception {rejected(login(a),a.getId(),null,403);}
    @Test void userIsolation() throws Exception {
        Inbox aa=new Inbox(),bb=new Inbox();connect(login(a),a.getId(),ORIGIN,aa);connect(login(b),b.getId(),ORIGIN,bb);
        service.sendNotificationToAuthor(a,"only-a");service.sendNotificationToAuthor(b,"only-b");
        assertThat(json.readTree(aa.take()).get("content").asText()).isEqualTo("only-a");assertThat(json.readTree(bb.take()).get("content").asText()).isEqualTo("only-b");
        assertThat(aa.messages.poll(150,TimeUnit.MILLISECONDS)).isNull();assertThat(bb.messages.poll(150,TimeUnit.MILLISECONDS)).isNull();
    }
    @Test void twoConnectionsAndCloseCleanup() throws Exception {
        HttpClient c=login(a);Inbox first=new Inbox(),second=new Inbox();WebSocket one=connect(c,a.getId(),ORIGIN,first);connect(c,a.getId(),ORIGIN,second);
        service.sendNotificationToAuthor(a,"both");first.take();second.take();
        one.sendClose(WebSocket.NORMAL_CLOSURE,"done").get(3,TimeUnit.SECONDS);assertThat(first.closed.get(3,TimeUnit.SECONDS)).isEqualTo(WebSocket.NORMAL_CLOSURE);
        service.sendNotificationToAuthor(a,"remaining");assertThat(json.readTree(second.take()).get("content").asText()).isEqualTo("remaining");assertThat(first.messages.poll(150,TimeUnit.MILLISECONDS)).isNull();
    }
    @ParameterizedTest @ValueSource(strings={"invalid-json","route"}) void clientCannotRouteMessages(String type) throws Exception {
        Inbox aa=new Inbox(),bb=new Inbox();WebSocket ws=connect(login(a),a.getId(),ORIGIN,aa);connect(login(b),b.getId(),ORIGIN,bb);
        ws.sendText(type.equals("route")?"{\"receiverId\":"+b.getId()+",\"content\":\"evil\"}":"not-json",true).get(3,TimeUnit.SECONDS);
        assertThat(aa.closed.get(3,TimeUnit.SECONDS)).isEqualTo(1008);assertThat(bb.messages.poll(150,TimeUnit.MILLISECONDS)).isNull();
        service.sendNotificationToAuthor(b,"legitimate");assertThat(json.readTree(bb.take()).get("content").asText()).isEqualTo("legitimate");
    }
    @Test void invalidatedSessionStopsPush() throws Exception {
        HttpClient c=login(a);Inbox box=new Inbox();connect(c,a.getId(),ORIGIN,box);
        c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/auth/test-expire-session")).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.discarding());
        service.sendNotificationToAuthor(a,"must-not-deliver");assertThat(box.closed.get(3,TimeUnit.SECONDS)).isEqualTo(1008);assertThat(box.messages.poll(150,TimeUnit.MILLISECONDS)).isNull();
    }
    @Test void deletedUserCookieRejected() throws Exception {HttpClient c=login(a);users.deleteById(a.getId());rejected(c,a.getId(),ORIGIN,401);}
    @Test void sessionSwitchStopsOldUserPush() throws Exception {
        HttpClient c=login(a);Inbox old=new Inbox();connect(c,a.getId(),ORIGIN,old);login(c,b);
        service.sendNotificationToAuthor(a,"old-user");assertThat(old.closed.get(3,TimeUnit.SECONDS)).isEqualTo(1008);assertThat(old.messages.poll(150,TimeUnit.MILLISECONDS)).isNull();
        Inbox current=new Inbox();connect(c,b.getId(),ORIGIN,current);service.sendNotificationToAuthor(b,"new-user");current.take();
    }
    @Test void serverPayloadCannotTargetAnotherUser() throws Exception {
        Inbox aa=new Inbox(),bb=new Inbox();connect(login(a),a.getId(),ORIGIN,aa);connect(login(b),b.getId(),ORIGIN,bb);
        Notifications payload=new Notifications();payload.setUser(a);payload.setContent("wrong-target");
        endpoint.sendNotification(b.getId().toString(),payload);endpoint.sendNotification(a.getId().toString(),null);
        assertThat(aa.messages.poll(150,TimeUnit.MILLISECONDS)).isNull();assertThat(bb.messages.poll(150,TimeUnit.MILLISECONDS)).isNull();
        service.sendNotificationToAuthor(a,"valid-a");service.sendNotificationToAuthor(b,"valid-b");aa.take();bb.take();
    }
    @Test void offlineHistoryStillAvailable() throws Exception {
        service.sendNotificationToAuthor(a,"offline-history");
        HttpClient c=login(a);
        var response=c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/notifications/"+a.getId())).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);assertThat(json.readTree(response.body()).get(0).get("content").asText()).isEqualTo("offline-history");
    }
    @ParameterizedTest @ValueSource(strings={"comments","like","bookmark"})
    void committedInteractionReachesRealSocket(String kind) throws Exception {
        HttpClient author=login(a),actor=login(b);Inbox box=new Inbox();connect(author,a.getId(),ORIGIN,box);
        String body=json.writeValueAsString(Map.of("title","synthetic article","content","synthetic content","visibility","PUBLIC","tags",List.of()));
        var created=author.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/articles"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(created.statusCode()).isEqualTo(200);Long articleId=json.readTree(created.body()).get("id").asLong();
        try {
            var operation=actor.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/articles/"+articleId+"/"+kind))
                    .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(kind.equals("comments")?"{\"content\":\"synthetic comment\"}":""))
                    .build(),HttpResponse.BodyHandlers.ofString());
            assertThat(operation.statusCode()).isEqualTo(200);
            var message=json.readTree(box.take());assertThat(message.get("user").get("id").asLong()).isEqualTo(a.getId());
            assertThat(message.get("createtime").asText()).contains("T");assertThat(notices.findByUserId(a.getId())).hasSize(1);
            String table=kind.equals("comments")?"comment":kind.equals("like")?"likes":"bookmark";
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `"+table+"` WHERE article_id=?",Long.class,articleId)).isEqualTo(1);
        } finally {articleService.deleteArticle(articleId,a.getId());}
    }
}
