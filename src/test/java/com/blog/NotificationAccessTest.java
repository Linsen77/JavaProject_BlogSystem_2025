package com.blog;

import com.blog.entity.*;
import com.blog.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
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
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.config.location=classpath:application-ownership-test.yml","spring.mail.username=synthetic@example.invalid"})
@AutoConfigureMockMvc
@Transactional
class NotificationAccessTest {
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){ArticleOwnershipTest.isolatedDatabase(r);}
    @MockBean JavaMailSender mail;
    @MockBean ServerEndpointExporter exporter;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired NotificationRepository notices;
    @Autowired EntityManager em;
    @Autowired com.blog.service.NotificationService service;
    User a,b;Notifications own,other;
    @BeforeEach void seed(){
        a=new User();a.setName("a");a.setEmail("a@example.invalid");a=users.saveAndFlush(a);
        b=new User();b.setName("b");b.setEmail("b@example.invalid");b=users.saveAndFlush(b);
        own=notice(a,"own-notice");other=notice(b,"OTHER-PRIVATE-NOTICE");
    }
    Notifications notice(User u,String text){Notifications n=new Notifications();n.setUser(u);n.setContent(text);n.setCreatetime(LocalDateTime.now());return notices.saveAndFlush(n);}
    MockHttpSession session(){MockHttpSession s=new MockHttpSession();s.setAttribute("loginUser",a);return s;}
    void unchanged(){em.flush();em.clear();assertThat(notices.count()).isEqualTo(2);assertThat(notices.findById(other.getId()).orElseThrow().isReadFlag()).isFalse();assertThat(notices.findById(own.getId()).orElseThrow().isReadFlag()).isFalse();}
    @AfterEach void noEmail(){org.mockito.Mockito.verifyNoInteractions(mail);}
    @Test void ownList() throws Exception {mvc.perform(get("/api/notifications/"+a.getId()).session(session())).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].content").value("own-notice"));}
    @Test void anonymousList401() throws Exception {mvc.perform(get("/api/notifications/"+a.getId())).andExpect(status().isUnauthorized());unchanged();}
    @Test void forgedList404() throws Exception {
        String body=mvc.perform(get("/api/notifications/"+b.getId()).session(session())).andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("OTHER-PRIVATE-NOTICE");unchanged();
    }
    @Test void ownRead() throws Exception {mvc.perform(put("/api/notifications/"+own.getId()+"/read").session(session())).andExpect(status().isOk());em.flush();em.clear();assertThat(notices.findById(own.getId()).orElseThrow().isReadFlag()).isTrue();assertThat(notices.findById(other.getId()).orElseThrow().isReadFlag()).isFalse();}
    @Test void otherRead404() throws Exception {mvc.perform(put("/api/notifications/"+other.getId()+"/read").session(session())).andExpect(status().isNotFound());unchanged();}
    @Test void anonymousRead401() throws Exception {mvc.perform(put("/api/notifications/"+own.getId()+"/read")).andExpect(status().isUnauthorized());unchanged();}
    @Test void missing404() throws Exception {mvc.perform(put("/api/notifications/999999/read").session(session())).andExpect(status().isNotFound());unchanged();}
    @ParameterizedTest @ValueSource(strings={"empty","stale","malformed"}) void invalidSession401(String type) throws Exception {
        MockHttpSession s=new MockHttpSession();User stale=new User();stale.setId(999999L);
        if(type.equals("stale"))s.setAttribute("loginUser",stale);if(type.equals("malformed"))s.setAttribute("loginUser","bad");
        mvc.perform(get("/api/notifications/"+a.getId()).session(s)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/notifications/"+own.getId()+"/read").session(s)).andExpect(status().isUnauthorized());unchanged();
    }
    @Test void serviceCannotSkipIdentity() {
        org.assertj.core.api.Assertions.assertThatThrownBy(()->service.markNotificationRead(other.getId(),null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .satisfies(e->assertThat(((org.springframework.web.server.ResponseStatusException)e).getStatusCode().value()).isEqualTo(401));
        unchanged();
    }
}
