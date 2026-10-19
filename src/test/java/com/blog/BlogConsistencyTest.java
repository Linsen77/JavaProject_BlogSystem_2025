package com.blog;

import com.blog.entity.Article;
import com.blog.entity.Bookmark;
import com.blog.entity.Comment;
import com.blog.entity.Likes;
import com.blog.entity.Notifications;
import com.blog.entity.Tag;
import com.blog.entity.User;
import com.blog.entity.UserViewHistory;
import com.blog.repository.*;
import com.blog.service.*;
import com.blog.controller.WebSocket.NotificationWebSocket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** No outer test transaction: all assertions observe real MySQL commits/rollbacks. */
@SpringBootTest(properties={"spring.config.location=classpath:application-ownership-test.yml","spring.mail.username=synthetic@example.invalid"})
@AutoConfigureMockMvc
class BlogConsistencyTest {
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){ArticleOwnershipTest.isolatedDatabase(r);}
    @MockBean JavaMailSender mail;
    @MockBean ServerEndpointExporter exporter;
    @MockBean NotificationWebSocket socket;
    @Autowired UserRepository users;
    @Autowired ArticleRepository articles;
    @Autowired TagRepository tags;
    @Autowired CommentRepository comments;
    @Autowired LikeRepository likes;
    @Autowired BookmarkRepository bookmarks;
    @Autowired UserViewHistoryRepository history;
    @Autowired NotificationRepository notices;
    @Autowired ArticleService articleService;
    @Autowired CommentService commentService;
    @Autowired LikeService likeService;
    @Autowired BookmarkService bookmarkService;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource source;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;
    User owner,other;Article a,b;Tag shared;
    @BeforeEach void seed(){
        reset(socket);
        owner=user("consistency-owner");other=user("consistency-other");
        shared=new Tag();shared.setName("shared-synthetic");shared=tags.saveAndFlush(shared);
        a=article(owner,"target");b=article(other,"preserved");attach(b,"all");
        for(User u:List.of(owner,other)){Notifications n=new Notifications();n.setUser(u);n.setContent("existing-history");n.setCreatetime(LocalDateTime.now());notices.saveAndFlush(n);}
    }
    User user(String name){User u=new User();u.setName(name);u.setEmail(name+"@example.invalid");return users.saveAndFlush(u);}
    Article article(User u,String title){Article x=new Article();x.setAuthor(u);x.setTitle(title);x.setContent("synthetic");x.setVisibility(Article.ArticleVisibility.PUBLIC);x.setTags(new ArrayList<>(List.of(shared)));return articles.saveAndFlush(x);}
    void attach(Article x,String kind){
        if(kind.equals("all")||kind.equals("comment")){Comment c=new Comment();c.setArticle(x);c.setUser(other);c.setContent("synthetic");comments.saveAndFlush(c);}
        if(kind.equals("all")||kind.equals("like-bookmark")){
            Likes l=new Likes();l.setArticle(x);l.setUser(other);likes.saveAndFlush(l);
            Bookmark bm=new Bookmark();bm.setArticle(x);bm.setUser(other);bookmarks.saveAndFlush(bm);
        }
        if(kind.equals("all")||kind.equals("history"))history.saveAndFlush(new UserViewHistory(other.getId(),x.getId(),LocalDateTime.now()));
    }
    long rows(String table,Long articleId){return jdbc.queryForObject("SELECT COUNT(*) FROM `"+table+"` WHERE article_id=?",Long.class,articleId);}
    long noticeCount(){return jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id=?",Long.class,owner.getId());}
    List<Long> snapshot(){List<Long> result=new ArrayList<>();for(Article x:List.of(a,b)){
        result.add(jdbc.queryForObject("SELECT COUNT(*) FROM article WHERE id=?",Long.class,x.getId()));
        for(String table:List.of("comment","likes","bookmark","user_view_history","article_tag"))result.add(rows(table,x.getId()));
    }result.add(noticeCount());return result;}
    MockHttpSession session(User u){MockHttpSession s=new MockHttpSession();s.setAttribute("loginUser",u);return s;}
    void preserved(){
        assertThat(articles.existsById(b.getId())).isTrue();assertThat(users.existsById(owner.getId())).isTrue();assertThat(users.existsById(other.getId())).isTrue();assertThat(tags.existsById(shared.getId())).isTrue();
        for(String table:List.of("comment","likes","bookmark","user_view_history","article_tag"))assertThat(rows(table,b.getId())).isEqualTo(1);
        assertThat(noticeCount()).isEqualTo(1);
    }
    String table(String kind){return switch(kind){case "comment"->"comment";case "like"->"likes";case "bookmark"->"bookmark";default->throw new IllegalArgumentException();};}
    Object interact(String kind){return switch(kind){case "comment"->commentService.addComment(a.getId(),other.getId(),"synthetic-comment");case "like"->likeService.addLike(a.getId(),other.getId());case "bookmark"->bookmarkService.addBookmark(a.getId(),other.getId());default->throw new IllegalArgumentException();};}
    void trigger(String name,String action,String table,String body){
        assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("blog_ownership_test");
        jdbc.execute("CREATE TRIGGER "+name+" BEFORE "+action+" ON `"+table+"` FOR EACH ROW "+body);
    }
    @AfterEach void cleanup(){
        for(String trigger:List.of("phase4_delete_failure","phase4_notice_failure","phase4_core_failure"))jdbc.execute("DROP TRIGGER IF EXISTS "+trigger);
        // Only exact IDs created in this synthetic fixture; never truncate shared tables.
        for(Article x:List.of(a,b)){
            for(String table:List.of("comment","likes","bookmark","user_view_history","article_tag"))jdbc.update("DELETE FROM `"+table+"` WHERE article_id=?",x.getId());
            jdbc.update("DELETE FROM article WHERE id=?",x.getId());
        }
        for(User u:List.of(owner,other)){jdbc.update("DELETE FROM notifications WHERE user_id=?",u.getId());users.deleteById(u.getId());}
        tags.deleteById(shared.getId());verifyNoInteractions(mail);
    }
    @ParameterizedTest @ValueSource(strings={"none","comment","like-bookmark","history","all"})
    void deletesOnlyArticleAssociations(String kind) throws Exception {
        attach(a,kind);mvc.perform(delete("/api/articles/"+a.getId()).session(session(owner))).andExpect(status().isOk());
        assertThat(articles.existsById(a.getId())).isFalse();
        for(String table:List.of("comment","likes","bookmark","user_view_history","article_tag"))assertThat(rows(table,a.getId())).isZero();preserved();
    }
    @Test void otherCannotDelete() throws Exception {attach(a,"all");var before=snapshot();mvc.perform(delete("/api/articles/"+a.getId()).session(session(other))).andExpect(status().isForbidden());assertThat(snapshot()).isEqualTo(before);}
    @Test void anonymousCannotDelete() throws Exception {attach(a,"all");var before=snapshot();mvc.perform(delete("/api/articles/"+a.getId())).andExpect(status().isUnauthorized());assertThat(snapshot()).isEqualTo(before);}
    @Test void missingDelete404() throws Exception {var before=snapshot();mvc.perform(delete("/api/articles/999999").session(session(owner))).andExpect(status().isNotFound());assertThat(snapshot()).isEqualTo(before);}
    @Test void deleteFailureRollsBackEarlierCleanup(){
        attach(a,"all");var before=snapshot();
        trigger("phase4_delete_failure","DELETE","bookmark","BEGIN IF OLD.article_id="+a.getId()+" THEN IF EXISTS(SELECT 1 FROM `comment` WHERE article_id="+a.getId()+") OR EXISTS(SELECT 1 FROM likes WHERE article_id="+a.getId()+") THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='cleanup-not-reached'; ELSE SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase4-delete-failure'; END IF; END IF; END");
        assertThatThrownBy(()->articleService.deleteArticle(a.getId(),owner.getId())).hasStackTraceContaining("phase4-delete-failure");
        assertThat(snapshot()).isEqualTo(before);preserved();
    }
    @ParameterizedTest @ValueSource(strings={"comment","like","bookmark"}) void noticeFailureRollsBackCore(String kind){
        trigger("phase4_notice_failure","INSERT","notifications","SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase4-notice-failure'");
        assertThatThrownBy(()->interact(kind)).hasStackTraceContaining("phase4-notice-failure");
        assertThat(rows(table(kind),a.getId())).isZero();assertThat(noticeCount()).isEqualTo(1);verifyNoInteractions(socket);
    }
    @ParameterizedTest @ValueSource(strings={"comment","like","bookmark"}) void failedCoreHasNoFalseNotice(String kind){
        trigger("phase4_core_failure","INSERT",table(kind),"SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase4-core-failure'");
        assertThatThrownBy(()->interact(kind)).hasStackTraceContaining("phase4-core-failure");
        assertThat(rows(table(kind),a.getId())).isZero();assertThat(noticeCount()).isEqualTo(1);verifyNoInteractions(socket);
    }
    @ParameterizedTest @ValueSource(strings={"comment","like","bookmark"}) void pushOnlyAfterCommittedRowsVisible(String kind){
        doAnswer(invocation->{
            try(Connection observer=source.getConnection();var statement=observer.createStatement()){
                try(var result=statement.executeQuery("SELECT COUNT(*) FROM `"+table(kind)+"` WHERE article_id="+a.getId())){result.next();assertThat(result.getLong(1)).isEqualTo(1);}
                try(var result=statement.executeQuery("SELECT COUNT(*) FROM notifications WHERE user_id="+owner.getId())){result.next();assertThat(result.getLong(1)).isEqualTo(2);}
            }return null;
        }).when(socket).sendNotification(anyString(),any(Notifications.class));
        new TransactionTemplate(transactions).executeWithoutResult(status->{interact(kind);verifyNoInteractions(socket);});
        verify(socket,times(1)).sendNotification(eq(owner.getId().toString()),any(Notifications.class));assertThat(noticeCount()).isEqualTo(2);
    }
    @ParameterizedTest @ValueSource(strings={"comment","like","bookmark"}) void rolledBackBusinessNeverPushes(String kind){
        new TransactionTemplate(transactions).executeWithoutResult(status->{interact(kind);verifyNoInteractions(socket);status.setRollbackOnly();});
        assertThat(rows(table(kind),a.getId())).isZero();assertThat(noticeCount()).isEqualTo(1);verifyNoInteractions(socket);
    }
    @ParameterizedTest @ValueSource(strings={"comment","like","bookmark"}) void socketFailureStillReturnsSuccessAndHistory(String kind) throws Exception {
        doThrow(new IllegalStateException("synthetic-send-failure")).when(socket).sendNotification(anyString(),any(Notifications.class));
        String suffix=kind.equals("comment")?"comments":kind;
        var request=post("/api/articles/"+a.getId()+"/"+suffix).session(session(other));
        if(kind.equals("comment"))request.contentType("application/json").content("{\"content\":\"synthetic\"}");
        mvc.perform(request).andExpect(status().isOk());assertThat(rows(table(kind),a.getId())).isEqualTo(1);assertThat(noticeCount()).isEqualTo(2);
        mvc.perform(get("/api/notifications/"+owner.getId()).session(session(owner))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }
    @ParameterizedTest @ValueSource(strings={"like","bookmark"}) void sequentialDuplicateDoesNotNotifyAgain(String kind){
        interact(kind);assertThatThrownBy(()->interact(kind)).isInstanceOf(RuntimeException.class);
        assertThat(rows(table(kind),a.getId())).isEqualTo(1);assertThat(noticeCount()).isEqualTo(2);verify(socket,times(1)).sendNotification(eq(owner.getId().toString()),any(Notifications.class));
    }
    @Test void distinctCommentSubmissionsAreTwoEvents(){interact("comment");interact("comment");assertThat(rows("comment",a.getId())).isEqualTo(2);assertThat(noticeCount()).isEqualTo(3);verify(socket,times(2)).sendNotification(eq(owner.getId().toString()),any(Notifications.class));}
    @Test void actualForeignKeysMatchModel(){
        List<String> refs=jdbc.query("SELECT TABLE_NAME,COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE CONSTRAINT_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME='article'",(rs,n)->rs.getString(1)+"."+rs.getString(2));
        assertThat(refs).containsExactlyInAnyOrder("comment.article_id","likes.article_id","bookmark.article_id","article_tag.article_id");
        List<String> rules=jdbc.queryForList("SELECT DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME='article'",String.class);
        assertThat(rules).hasSize(4).allSatisfy(rule->assertThat(rule).isIn("RESTRICT","NO ACTION"));
        System.out.println("Isolated Article FK references: "+refs+"; delete rules: "+rules+"; history uses scalar article_id, no FK");
    }
    @Test void failedHistoryWriteMustNotLeaveViewCount() {
        trigger("phase4_core_failure","INSERT","user_view_history","SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase4-history-failure'");
        assertThatThrownBy(()->mvc.perform(get("/api/articles/"+a.getId()).session(session(other))))
                .hasStackTraceContaining("phase4-history-failure");
        assertThat(jdbc.queryForObject("SELECT view_count FROM article WHERE id=?",Integer.class,a.getId())).isZero();
        assertThat(rows("user_view_history",a.getId())).isZero();
    }
}
