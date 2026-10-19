package com.blog;

import com.blog.config.WebConfig;
import com.blog.controller.UserController;
import com.blog.entity.User;
import com.blog.repository.*;
import com.blog.service.*;
import java.nio.file.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AvatarUploadTest {
    @TempDir Path temporary;
    static MockMultipartFile image() { return new MockMultipartFile("avatar", "ignored.png", "image/png", new byte[]{1,2,3}); }

    // Separate JVM ensures the real default relative directory is tested without
    // touching the application's working directory or changing global user.dir.
    public static class DefaultDirectoryProbe {
        public static void main(String[] args) throws Exception {
            AvatarStorage storage = new AvatarStorage("./data/avatars");
            String url = storage.save(image());
            if (!Files.exists(Path.of("data/avatars").resolve(url.substring("/avatar/".length()))))
                throw new AssertionError("Default directory did not save avatar");
        }
    }
    @Test void defaultDirectorySavesInTemporaryWorkingDirectory() throws Exception {
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),
                "-cp", System.getProperty("java.class.path"), DefaultDirectoryProbe.class.getName())
                .directory(temporary.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertThat(process.waitFor()).as(output).isZero();
        try (var files = Files.list(temporary.resolve("data/avatars"))) { assertThat(files.count()).isEqualTo(1); }
    }
    @Test void customMissingDirectoryCreatedAndRandomNamesPreserved() throws Exception {
        AvatarStorage storage = new AvatarStorage(temporary.resolve("nested/custom").toString());
        String first = storage.save(image()), second = storage.save(image());
        assertThat(first).matches("/avatar/[0-9a-f-]{36}\\.png").isNotEqualTo(second);
        assertThat(Files.readAllBytes(storage.directory().resolve(first.substring(8)))).containsExactly(1,2,3);
    }
    @Test void relativeDirectoryWorksWithoutWindowsPath() throws Exception {
        Path target = temporary.resolve("relative/avatars");
        Path relative = Path.of("").toAbsolutePath().relativize(target);
        AvatarStorage storage = new AvatarStorage(relative.toString());
        storage.save(image());
        assertThat(storage.directory()).isEqualTo(target);
    }
    @Test void partialFileIsRemovedOnTransferFailure() throws Exception {
        AvatarStorage storage = new AvatarStorage(temporary.toString());
        var broken = mock(org.springframework.web.multipart.MultipartFile.class);
        doAnswer(call -> {Files.write(call.getArgument(0,Path.class),new byte[]{9});throw new java.io.IOException("synthetic");})
                .when(broken).transferTo(any(Path.class));
        assertThatThrownBy(() -> storage.save(broken)).isInstanceOf(java.io.IOException.class);
        try(var files=Files.list(temporary)) {assertThat(files.count()).isZero();}
    }

    @org.springframework.boot.test.context.TestConfiguration @EnableWebMvc @Import({UserController.class,WebConfig.class})
    static class WebTestConfig {
        @Bean AvatarStorage avatars(org.springframework.core.env.Environment env) {return new AvatarStorage(env.getRequiredProperty("test.avatar.directory"));}
    }
    void verifyUpload(Path directory, boolean success) throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.addBeanFactoryPostProcessor(factory -> {
                for(Class<?> type: new Class<?>[]{UserRepository.class,ArticleRepository.class,FollowService.class,
                        ArticleService.class,ArticleReadService.class,UserStatisticsService.class,BookmarkService.class})
                    factory.registerSingleton(type.getSimpleName(),mock(type));
            });
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("avatar-test",java.util.Map.of("test.avatar.directory",directory.toString())));
            context.register(WebTestConfig.class); context.refresh();
            MockMvc mvc=MockMvcBuilders.webAppContextSetup(context).build();
            User user=new User();user.setId(7L);user.setName("synthetic");
            UserRepository users=context.getBean(UserRepository.class);
            when(users.findById(7L)).thenReturn(Optional.of(user));when(users.save(any(User.class))).thenAnswer(c->c.getArgument(0));
            MockHttpSession session=new MockHttpSession();session.setAttribute("loginUser",user);
            var request=multipart("/api/users/7").file(image())
                    .file(new MockMultipartFile("name","","text/plain","synthetic".getBytes()))
                    .file(new MockMultipartFile("bio","","text/plain","bio".getBytes()))
                    .session(session).with(r->{r.setMethod("PUT");return r;});
            if(success) {
                mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.startsWith("/avatar/")));
                mvc.perform(get(user.getAvatarUrl())).andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1,2,3}));
            } else {
                mvc.perform(request).andExpect(status().isInternalServerError()).andExpect(content().string("头像上传失败"));
                verify(users,never()).save(any());
            }
        }
    }
    @Test void uploadUrlReadsSavedFileThroughResourceMapping() throws Exception {verifyUpload(temporary.resolve("new/avatars"),true);}
    @Test void unavailableDirectoryReturnsSafeErrorWithoutSavingUser() throws Exception {
        Path file=Files.writeString(temporary.resolve("not-a-directory"),"synthetic");verifyUpload(file,false);
    }
}
