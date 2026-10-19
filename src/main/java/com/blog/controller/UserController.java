package com.blog.controller;

import com.blog.dto.UserProfileDTO;
import com.blog.dto.UserStatisticsDto;
import com.blog.entity.Article;
import com.blog.service.AvatarStorage;
import com.blog.entity.User;
import com.blog.repository.ArticleRepository;
import com.blog.repository.UserRepository;
import com.blog.service.ArticleService;
import com.blog.service.ArticleReadService;
import com.blog.service.BookmarkService;
import com.blog.service.FollowService;
import com.blog.service.UserStatisticsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.time.LocalDateTime;
import java.util.List;

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
    @Autowired private ArticleReadService reads;
    @Autowired private AvatarStorage avatarStorage;

    @Autowired
    private UserStatisticsService userStatisticsService;

    @Autowired
    private BookmarkService bookmarkService;

    //访问某个用户主页
    @GetMapping("/{userId}")
    public UserProfileDTO getUserProfile(@PathVariable Long userId,@RequestParam(required=false) Long viewerId,HttpSession session) {
        User user=userRepository.findById(userId).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"用户不存在"));
        return new UserProfileDTO(user,reads.list(ArticleReadService.Scope.AUTHOR,userId,null,reads.viewerId(session)));
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
    public ResponseEntity<UserStatisticsDto> getUserStatistics(@PathVariable Long userId,HttpSession session) {
        return ResponseEntity.ok(userStatisticsService.getUserStatistics(userId,reads.viewerId(session)));
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
            try {
                user.setAvatarUrl(avatarStorage.save(avatar));
            } catch (Exception e) {
                return ResponseEntity.status(500)
                        .contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                        .body("头像上传失败");
            }
        }

        user.setUpdateAt(LocalDateTime.now());
        userRepository.save(user);

        // 更新 session
        session.setAttribute("loginUser", user);



        return ResponseEntity.ok(user);
    }

    //查看用户收藏的文章
    @GetMapping("/bookmarks/{userId}")
    public ResponseEntity<List<Article>> getUserBookmarks(@PathVariable Long userId,HttpSession session) {
        return ResponseEntity.ok(reads.list(ArticleReadService.Scope.BOOKMARKS,userId,null,reads.viewerId(session)));
    }


}
