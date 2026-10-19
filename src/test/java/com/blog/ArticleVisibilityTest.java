package com.blog;

import com.blog.entity.*;
import com.blog.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import com.blog.service.ArticleReadService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.util.*;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.config.location=classpath:application-ownership-test.yml","spring.mail.username=synthetic@example.invalid"})
@AutoConfigureMockMvc
@Transactional
class ArticleVisibilityTest {
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){ArticleOwnershipTest.isolatedDatabase(r);}
    @MockBean JavaMailSender mail;
    @MockBean ServerEndpointExporter exporter;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ArticleRepository articles;
    @Autowired TagRepository tags;
    @Autowired FollowRepository follows;
    @Autowired BookmarkRepository bookmarks;
    @Autowired CommentRepository comments;
    @Autowired LikeRepository likes;
    @Autowired UserViewHistoryRepository history;
    @Autowired EntityManager em;
    @Autowired ArticleReadService reads;
    User owner,fan,stranger,reverse;
    Article pub,pub2,priv,restricted,alien;
    Tag tag;
    User user(String name){User u=new User();u.setName(name);u.setEmail(name+"@example.invalid");return users.saveAndFlush(u);}
    Article article(User author,String title,Article.ArticleVisibility visibility){
        Article a=new Article();a.setAuthor(author);a.setTitle(title);a.setContent(title+"-body");a.setCover(title+"-attachment");
        a.setVisibility(visibility);a.setCreateTime(LocalDateTime.now());a.setTags(new ArrayList<>(List.of(tag)));return articles.saveAndFlush(a);
    }
    void follow(User from,User to){Follows f=new Follows();f.setFollowerId(from.getId());f.setFolloweeId(to.getId());follows.saveAndFlush(f);}
    @BeforeEach void seed(){
        owner=user("visibility-author");fan=user("fan");stranger=user("stranger");reverse=user("reverse");
        tag=new Tag();tag.setName("visibility-tag");tag=tags.saveAndFlush(tag);
        pub=article(owner,"visible-public",Article.ArticleVisibility.PUBLIC);
        pub2=article(owner,"visible-public-two",Article.ArticleVisibility.PUBLIC);
        priv=article(owner,"PRIVATE-SECRET",Article.ArticleVisibility.PRIVATE);
        Tag privateTag=new Tag();privateTag.setName("PRIVATE-TAG-SECRET");privateTag=tags.saveAndFlush(privateTag);
        priv.getTags().add(privateTag);articles.saveAndFlush(priv);
        restricted=article(owner,"FOLLOWERS-SECRET",Article.ArticleVisibility.FOLLOWERS);
        alien=article(stranger,"ALIEN-SECRET",Article.ArticleVisibility.PRIVATE);
        follow(fan,owner);follow(owner,reverse);
        for(Article a:List.of(pub,priv,restricted)){
            Bookmark b=new Bookmark();b.setUser(fan);b.setArticle(a);bookmarks.saveAndFlush(b);
            Comment c=new Comment();c.setUser(fan);c.setArticle(a);c.setContent(a.getTitle()+"-comment");comments.saveAndFlush(c);
            Likes l=new Likes();l.setUser(fan);l.setArticle(a);likes.saveAndFlush(l);
        }
    }
    MockHttpSession session(User u){MockHttpSession s=new MockHttpSession();if(u!=null)s.setAttribute("loginUser",u);return s;}
    User actor(String who){return switch(who){case "author"->owner;case "fan"->fan;case "reverse"->reverse;case "stranger"->stranger;default->null;};}
    String url(String kind){return switch(kind){
        case "all"->"/api/articles";
        case "author"->"/api/articles/byAuthor/"+owner.getId();
        case "profile"->"/api/users/"+owner.getId()+"?viewerId="+owner.getId();
        case "search"->"/api/articles/search/byAuthor?authorName=visibility-author";
        case "title"->"/api/articles/search/byTitle?keyword=SECRET";
        case "tag"->"/api/articles/search/byTag?tagName=visibility-tag";
        case "bookmarks"->"/api/users/bookmarks/"+fan.getId();
        case "feed"->"/api/follow/articles/"+fan.getId();
        default->throw new IllegalArgumentException(kind);};}
    void noSecrets(String body){assertThat(body).doesNotContain("PRIVATE-SECRET","FOLLOWERS-SECRET","ALIEN-SECRET","PRIVATE-TAG-SECRET");}
    @AfterEach void noEmail(){org.mockito.Mockito.verifyNoInteractions(mail);}
    @ParameterizedTest @ValueSource(strings={"anonymous","author","fan","stranger","reverse"})
    void detailMatrix(String who) throws Exception {
        User u=actor(who);MockHttpSession s=session(u);
        mvc.perform(get("/api/articles/"+pub.getId()).session(s)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value(pub.getTitle()));
        for(Article a:List.of(priv,restricted)){
            boolean allowed=u==owner || a==restricted && u==fan;
            var result=mvc.perform(get("/api/articles/"+a.getId()).session(s)).andExpect(allowed?status().isOk():status().isNotFound()).andReturn();
            if(!allowed)assertThat(result.getResponse().getContentAsString()).doesNotContain(a.getTitle(),a.getContent(),a.getCover());
        }
    }
    @ParameterizedTest @ValueSource(strings={"all","author","profile","search","title","tag","bookmarks","feed"})
    void anonymousCannotReadViaLists(String kind) throws Exception {
        noSecrets(mvc.perform(get(url(kind))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    @ParameterizedTest @ValueSource(strings={"all","author","profile","search","title","tag","bookmarks","feed"})
    void fanCannotReadPrivateViaLists(String kind) throws Exception {
        String body=mvc.perform(get(url(kind)).session(session(fan))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("PRIVATE-SECRET","ALIEN-SECRET");
        assertThat(body).contains("FOLLOWERS-SECRET");
    }
    @ParameterizedTest @ValueSource(strings={"","/tags","/comments","/likes?userId=1","/likes/count","/bookmark/status"})
    void privateSubresources404(String suffix) throws Exception {
        String body=mvc.perform(get("/api/articles/"+priv.getId()+suffix).session(session(fan)))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();noSecrets(body);
    }
    @ParameterizedTest @ValueSource(strings={"like","bookmark","comments"})
    void interactionResponseCannotLeak(String kind) throws Exception {
        long b=bookmarks.count(),c=comments.count(),l=likes.count();
        var request=post("/api/articles/"+priv.getId()+"/"+kind).session(session(stranger));
        if(kind.equals("comments"))request.contentType("application/json").content("{\"content\":\"synthetic\"}");
        noSecrets(mvc.perform(request).andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString());
        em.flush();assertThat(bookmarks.count()).isEqualTo(b);assertThat(comments.count()).isEqualTo(c);assertThat(likes.count()).isEqualTo(l);
    }
    @Test void deniedDetailHasNoSideEffects() throws Exception {
        long count=history.count();int views=priv.getViewCount();
        mvc.perform(get("/api/articles/"+priv.getId()).session(session(fan))).andExpect(status().isNotFound());
        em.flush();em.clear();assertThat(history.count()).isEqualTo(count);assertThat(articles.findById(priv.getId()).orElseThrow().getViewCount()).isEqualTo(views);
    }
    @Test void ownerProfileOnlyOwnArticles() throws Exception {
        String body=mvc.perform(get(url("profile")).session(session(owner))).andExpect(status().isOk()).andExpect(jsonPath("$.articles.length()").value(4))
                .andReturn().getResponse().getContentAsString();assertThat(body).doesNotContain("ALIEN-SECRET");
    }
    @ParameterizedTest @ValueSource(strings={"stale","malformed","empty"})
    void invalidSessionIsAnonymous(String type) throws Exception {
        MockHttpSession s=new MockHttpSession();User stale=new User();stale.setId(999999L);
        if(type.equals("stale"))s.setAttribute("loginUser",stale);if(type.equals("malformed"))s.setAttribute("loginUser","bad");
        noSecrets(mvc.perform(get(url("profile")).session(s)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/articles/"+priv.getId()).session(s)).andExpect(status().isNotFound());
        mvc.perform(get("/api/articles/"+pub.getId()).session(s)).andExpect(status().isOk());
    }
    @Test void missing404MatchesDenied() throws Exception {
        mvc.perform(get("/api/articles/999999")).andExpect(status().isNotFound());
    }
    @Test void publicCommentsKeepShape() throws Exception {
        mvc.perform(get("/api/articles/"+pub.getId()+"/comments")).andExpect(status().isOk()).andExpect(jsonPath("$[0].article.id").value(pub.getId()));
    }
    @Test void hotLimitStillArray() throws Exception {
        mvc.perform(get("/api/articles/hot?limit=1")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void anonymousStatisticsExcludeHidden() throws Exception {
        mvc.perform(get("/api/users/"+owner.getId()+"/status")).andExpect(status().isOk()).andExpect(jsonPath("$.articleCount").value(2)).andExpect(jsonPath("$.likeCount").value(1));
        mvc.perform(get("/api/users/"+fan.getId()+"/status")).andExpect(status().isOk()).andExpect(jsonPath("$.bookmarks").value(1));
    }
    @Test void recommendationCannotUseClaimedViewer() throws Exception {
        noSecrets(mvc.perform(get("/api/articles/recommend/"+owner.getId())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    @Test void recommendationUsesSessionNotPath() throws Exception {
        history.saveAndFlush(new UserViewHistory(fan.getId(),pub.getId(),LocalDateTime.now()));
        mvc.perform(get("/api/articles/recommend/"+owner.getId()).session(session(fan)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(pub2.getId()));
    }
    @Test void likeStatusUsesSessionNotParameter() throws Exception {
        mvc.perform(get("/api/articles/"+pub.getId()+"/likes?userId="+fan.getId()).session(session(stranger)))
                .andExpect(status().isOk()).andExpect(content().string("false"));
        mvc.perform(get("/api/articles/"+pub.getId()+"/likes?userId="+owner.getId()).session(session(fan)))
                .andExpect(status().isOk()).andExpect(content().string("true"));
    }
    @Test void allowedNestedResponses() throws Exception {
        mvc.perform(get("/api/articles/"+restricted.getId()+"/comments").session(session(fan)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].article.id").value(restricted.getId()));
        mvc.perform(get("/api/articles/"+priv.getId()+"/tags").session(session(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }
    static java.util.stream.Stream<Arguments> pages(){
        return java.util.stream.Stream.of("anonymous","author","fan").flatMap(who->Arrays.stream(ArticleReadService.Scope.values()).map(scope->Arguments.of(who,scope)));
    }
    @ParameterizedTest @MethodSource("pages")
    void sqlPagesAndTotals(String who,ArticleReadService.Scope scope){
        User u=actor(who);Long viewer=u==null?null:u.getId();
        Long target=scope==ArticleReadService.Scope.AUTHOR?owner.getId():scope==ArticleReadService.Scope.BOOKMARKS?fan.getId():scope==ArticleReadService.Scope.FEED?viewer:null;
        String term=scope==ArticleReadService.Scope.TITLE?"SECRET":scope==ArticleReadService.Scope.TAG?"visibility-tag":scope==ArticleReadService.Scope.AUTHOR_NAME?"visibility-author":null;
        List<Long> expected=new ArrayList<>(List.of(pub.getId(),pub2.getId()));
        if(who.equals("author"))expected.add(priv.getId());
        if(!who.equals("anonymous"))expected.add(restricted.getId());
        if(scope==ArticleReadService.Scope.TITLE){expected.remove(pub.getId());expected.remove(pub2.getId());}
        if(scope==ArticleReadService.Scope.BOOKMARKS)expected.remove(pub2.getId());
        if(scope==ArticleReadService.Scope.FEED && !who.equals("fan"))expected.clear();
        expected.sort(Comparator.reverseOrder());List<Long> actual=new ArrayList<>();
        for(int index=0;index<Math.max(1,expected.size());index++){
            var page=reads.page(scope,target,term,viewer,PageRequest.of(index,1,Sort.by(Sort.Direction.DESC,"id")));
            assertThat(page.getTotalElements()).isEqualTo(expected.size());
            assertThat(page.getContent()).hasSize(expected.isEmpty()?0:1);
            page.getContent().forEach(a->actual.add(a.getId()));
        }
        assertThat(actual).containsExactlyElementsOf(expected);
        assertThat(reads.page(scope,target,term,viewer,PageRequest.of(expected.size()+1,1)).getContent()).isEmpty();
    }
}
