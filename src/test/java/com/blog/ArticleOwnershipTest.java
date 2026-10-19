package com.blog;

import com.blog.entity.*;
import com.blog.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import jakarta.persistence.EntityManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.http.MediaType;
import java.util.ArrayList;

@SpringBootTest(properties={"spring.config.location=classpath:application-ownership-test.yml", "spring.mail.username=synthetic@example.invalid"})
@AutoConfigureMockMvc
@Transactional
class ArticleOwnershipTest {
    @DynamicPropertySource static void isolatedDatabase(DynamicPropertyRegistry r) {
        String port=System.getenv("BLOG_TEST_PORT");
        if (!"isolated-blog-ownership".equals(System.getenv("BLOG_TEST_GUARD")) || port==null || !port.matches("[0-9]+") || Integer.parseInt(port)<1024 || port.equals("3306"))
            throw new IllegalStateException("Use scripts/run-ownership-tests.py only");
        r.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:"+port+"/blog_ownership_test?allowPublicKeyRetrieval=true&useSSL=false");
        r.add("spring.datasource.username",()->"root");r.add("spring.datasource.password",()->"ownership-test-only");
    }
    @MockBean JavaMailSender mail;
    @MockBean ServerEndpointExporter exporter;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ArticleRepository articles;
    @Autowired TagRepository tags;
    @Autowired EntityManager em;
    @Autowired com.blog.service.ArticleService service;
    @AfterEach void noExternalEmail() {org.mockito.Mockito.verifyNoInteractions(mail);}
    User owner, other;Article article;Tag tag;
    @BeforeEach void seed() {
        owner=new User();owner.setName("owner");owner.setEmail("owner@example.invalid");owner=users.saveAndFlush(owner);
        other=new User();other.setName("other");other.setEmail("other@example.invalid");other=users.saveAndFlush(other);
        tag=new Tag();tag.setName("synthetic");tag=tags.saveAndFlush(tag);
        article=new Article();article.setAuthor(owner);article.setTitle("original");article.setContent("synthetic");
        article.setVisibility(Article.ArticleVisibility.PUBLIC);article.setTags(new ArrayList<>());article=articles.saveAndFlush(article);
    }
    MockHttpSession session(User u) {MockHttpSession s=new MockHttpSession();s.setAttribute("loginUser",u);return s;}
    String body(Long id, Long author) {return "{\"id\":"+id+",\"authorId\":"+author+",\"title\":\"changed\",\"content\":\"synthetic\",\"visibility\":\"PUBLIC\",\"tags\":[\"new-tag\"]}";}
    MockHttpServletRequestBuilder write(String op,long id) {
        String path="/api/articles/"+id;
        return op.equals("update")?put(path).contentType(MediaType.APPLICATION_JSON).content(body(null,other.getId())):
            op.equals("delete")?delete(path):post(path+"/tags").contentType(MediaType.APPLICATION_JSON).content("["+tag.getId()+"]");
    }
    void unchanged() {
        em.flush();em.clear();Article a=articles.findById(article.getId()).orElseThrow();
        assertThat(a.getTitle()).isEqualTo("original");assertThat(a.getAuthor().getId()).isEqualTo(owner.getId());
        assertThat(a.getTags()).isEmpty();assertThat(articles.count()).isEqualTo(1);assertThat(tags.count()).isEqualTo(1);
    }
    @Test void anonymousCreateRejected() throws Exception {
        mvc.perform(post("/api/articles").contentType(MediaType.APPLICATION_JSON).content(body(null,owner.getId()))).andExpect(status().isUnauthorized());unchanged();
    }
    @Test void ownerCreates() throws Exception {
        mvc.perform(post("/api/articles").session(session(owner)).contentType(MediaType.APPLICATION_JSON).content(body(null,owner.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.author.id").value(owner.getId()));em.flush();assertThat(articles.count()).isEqualTo(2);
    }
    @Test void forgedAuthorIgnored() throws Exception {
        mvc.perform(post("/api/articles").session(session(owner)).contentType(MediaType.APPLICATION_JSON).content(body(null,other.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.author.id").value(owner.getId()));
        em.flush();em.clear();assertThat(articles.findAll()).allMatch(a->a.getAuthor().getId().equals(owner.getId()));
    }
    @Test void authorCanBeOmitted() throws Exception {
        mvc.perform(post("/api/articles").session(session(owner)).contentType(MediaType.APPLICATION_JSON).content(body(null,null)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.author.id").value(owner.getId()));
    }
    @ParameterizedTest @ValueSource(strings={"update","delete","tags"}) void ownerWrites(String op) throws Exception {
        mvc.perform(write(op,article.getId()).session(session(owner))).andExpect(status().isOk());em.flush();em.clear();
        if(op.equals("delete"))assertThat(articles.existsById(article.getId())).isFalse();
        else {Article a=articles.findById(article.getId()).orElseThrow();assertThat(a.getAuthor().getId()).isEqualTo(owner.getId());
            if(op.equals("update"))assertThat(a.getTitle()).isEqualTo("changed");else assertThat(a.getTags()).hasSize(1);}
    }
    @ParameterizedTest @ValueSource(strings={"update","delete","tags"}) void otherCannotWrite(String op) throws Exception {
        mvc.perform(write(op,article.getId()).session(session(other))).andExpect(status().isForbidden());unchanged();
    }
    @ParameterizedTest @ValueSource(strings={"owner","other"}) void postCannotUpdate(String who) throws Exception {
        User u=who.equals("owner")?owner:other;
        mvc.perform(post("/api/articles").session(session(u)).contentType(MediaType.APPLICATION_JSON).content(body(article.getId(),u.getId())))
                .andExpect(status().isBadRequest());unchanged();
    }
    @ParameterizedTest @ValueSource(strings={"update","delete","tags"}) void missingArticle404(String op) throws Exception {
        mvc.perform(write(op,999999).session(session(owner))).andExpect(status().isNotFound());unchanged();
    }
    @ParameterizedTest @ValueSource(strings={"update","delete","tags"}) void anonymousWrites401(String op) throws Exception {
        mvc.perform(write(op,article.getId())).andExpect(status().isUnauthorized());unchanged();
    }
    @Test void staleSessionRejected() throws Exception {
        User stale=new User();stale.setId(999999L);
        mvc.perform(post("/api/articles").session(session(stale)).contentType(MediaType.APPLICATION_JSON).content(body(null,owner.getId())))
                .andExpect(status().isUnauthorized());unchanged();
    }
    @Test void malformedSessionRejected() throws Exception {
        MockHttpSession s=new MockHttpSession();s.setAttribute("loginUser","not-a-user");
        mvc.perform(write("delete",article.getId()).session(s)).andExpect(status().isUnauthorized());unchanged();
    }
    @Test void serviceAlsoRejectsMissingIdentity() {
        org.assertj.core.api.Assertions.assertThatThrownBy(()->service.deleteArticle(article.getId(),null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .satisfies(e->assertThat(((org.springframework.web.server.ResponseStatusException)e).getStatusCode().value()).isEqualTo(401));
        unchanged();
    }
    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void committedRequestsAndTagRollback() throws Exception {
        Long created=null;
        try {
            mvc.perform(post("/api/articles/"+article.getId()+"/tags").session(session(owner)).contentType(MediaType.APPLICATION_JSON)
                    .content("["+tag.getId()+",999999]")).andExpect(status().isNotFound());
            assertThat(((Number)em.createNativeQuery("SELECT COUNT(*) FROM article_tag WHERE article_id="+article.getId()).getSingleResult()).intValue()).isZero();
            String response=mvc.perform(post("/api/articles").session(session(owner)).contentType(MediaType.APPLICATION_JSON).content(body(null,other.getId())))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            created=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asLong();
            assertThat(articles.findById(created).orElseThrow().getAuthor().getId()).isEqualTo(owner.getId());
            mvc.perform(write("update",created).session(session(owner))).andExpect(status().isOk());
            assertThat(articles.findById(created).orElseThrow().getTitle()).isEqualTo("changed");
            mvc.perform(write("delete",created).session(session(other))).andExpect(status().isForbidden());
            assertThat(articles.existsById(created)).isTrue();
            mvc.perform(write("delete",created).session(session(owner))).andExpect(status().isOk());
            assertThat(articles.existsById(created)).isFalse();
        } finally {
            // Only exact synthetic IDs from this test; never truncate or deleteAll.
            if(created!=null && articles.existsById(created)) articles.deleteById(created);
            articles.deleteById(article.getId());
            tags.findByName("new-tag").ifPresent(tags::delete);
            tags.deleteById(tag.getId());users.deleteById(owner.getId());users.deleteById(other.getId());
        }
    }
}
