package com.blog.controller;

import com.blog.dto.UserProfileDTO;
import com.blog.dto.UserStatisticsDto;
import com.blog.dto.UserUpdateDTO;
import com.blog.entity.Article;
import java.io.File;
import com.blog.entity.User;
import com.blog.repository.ArticleRepository;
import com.blog.repository.UserRepository;
import com.blog.service.ArticleService;
import com.blog.service.FollowService;
import com.blog.service.UserStatisticsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private FollowService followService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserStatisticsService userStatisticsService;

    //访问某个用户主页
    @GetMapping("/{userId}")
    public UserProfileDTO getUserProfile(@PathVariable Long userId,  @RequestParam(required = false) Long viewerId){

        User user = userRepository.findById(userId).orElseThrow(()->new RuntimeException("User not found"));
        List<Article> articles;

        //自己查看自己的主页：查看所有文章
        if(viewerId != null && viewerId.equals(user.getId())) {
            articles = articleService.getAllArticles();
        }
        //粉丝查看：公开+粉丝可见的文章
        else if(viewerId != null && followService.isFollowing(viewerId, user.getId())) {
            articles = articleService.findVisibleToFollowers(userId);
        }
        //陌生人查看：公开的文章
        else{
            articles = articleService.findByAuthorIdAndVisibility(userId, Article.ArticleVisibility.PUBLIC);
        }

        return new UserProfileDTO(user,articles);
    }

    //获取当前登录用户信息
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session){
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body("未登录");
        }
        return ResponseEntity.ok(loginUser);
    }

    //获取用户的文章数，粉丝数和点赞数
    @GetMapping("/{userId}/status")
    public ResponseEntity<UserStatisticsDto> getUserStatistics(@PathVariable Long userId) {
        UserStatisticsDto statistics = userStatisticsService.getUserStatistics(userId);
        return ResponseEntity.ok(statistics);
    }

    // 更新当前用户资料
    @PutMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(
            @PathVariable Long userId,
            @RequestPart("name") String name,
            @RequestPart("bio") String bio,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            HttpSession session
    ) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body("未登录");
        }

        if (!loginUser.getId().equals(userId)) {
            return ResponseEntity.status(403).body("不能修改其他用户资料");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setName(name);
        user.setBio(bio);

        // 如果上传了头像
        if (avatar != null && !avatar.isEmpty()) {
            String fileName = UUID.randomUUID().toString() + ".png";
            String path = "C:/Users/33132/Desktop/Java_2025/BlogSystem/avatar/" + fileName;
            try {
                avatar.transferTo(new File(path));
            } catch (Exception e) {
                return ResponseEntity.status(500).body("头像上传失败");
            }
            user.setAvatarUrl("http://localhost:8080/avatar/" + fileName);
        }

        user.setUpdateAt(LocalDateTime.now());
        userRepository.save(user);

        // 更新 session
        session.setAttribute("loginUser", user);

        System.out.println("avatar = " + avatar);


        return ResponseEntity.ok(user);
    }

}