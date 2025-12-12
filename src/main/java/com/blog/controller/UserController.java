package com.blog.controller;

import com.blog.dto.UserProfileDTO;
import com.blog.dto.UserUpdateDTO;
import com.blog.entity.Article;
import java.io.File;
import com.blog.entity.User;
import com.blog.repository.ArticleRepository;
import com.blog.repository.UserRepository;
import com.blog.service.ArticleService;
import com.blog.service.FollowService;
import com.blog.service.UserService;
import org.springframework.aot.generate.AccessControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private FollowService followService;

    @Autowired
    private ArticleService articleService;

    //访问某个用户主页
    @GetMapping("/{userId}/profile")
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
    public User getCurrentUser(Principal principal){
        return userRepository.findByEmail(principal.getName());
    }

    //更新当前用户资料
    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(@RequestBody UserUpdateDTO dto, Principal principal){
        User user = userRepository.findByEmail(principal.getName());

        user.setName(dto.getUsername());
        user.setBio(dto.getBio());

        userRepository.save(user);
        return ResponseEntity.ok("更新完成ovo");

    }

    //上传头像
    @PostMapping("/uploadAvatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, Principal principal){
        String fileName = UUID.randomUUID().toString() + ".png";
        String path = "C:/Users/33132/Desktop/Java_2025/BlogSystem/avatar/" + fileName;

        try{
            file.transferTo(new File(path));
        }
        catch (Exception e){
            return ResponseEntity.status(500).body("头像上传失败o(╥﹏╥)o");
        }

        User user = userRepository.findByEmail(principal.getName());
        user.setAvatarUrl("/avatar"+fileName);
        userRepository.save(user);

        return ResponseEntity.ok("头像上传成功！");
    }

}