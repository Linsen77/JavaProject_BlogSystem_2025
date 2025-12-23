package com.blog.controller;


import com.blog.entity.Article;
import com.blog.entity.Follows;
import com.blog.entity.User;
import com.blog.repository.ArticleRepository;
import com.blog.repository.FollowRepository;
import com.blog.repository.UserRepository;
import com.blog.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ArticleRepository articleRepository;

    //关注某个用户
    @PostMapping("/follow/{targetUserId}")
    public ResponseEntity<?> follow(@PathVariable Long targetUserId, Principal principal) {
        User me = userRepository.findByEmail(principal.getName());

        if(me.getId().equals(targetUserId)){
            return ResponseEntity.badRequest().body("不能关注自己！！！！");
        }

        boolean ok = followService.follow(me.getId(), targetUserId);
        return ResponseEntity.ok(ok ? "关注成功" : "已关注该用户");
    }

    //取消关注
    @DeleteMapping("/follow/{targetUserId}")
    public ResponseEntity<?> unfollow(@PathVariable Long targetUserId, Principal principal) {
        User me = userRepository.findByEmail(principal.getName());

        boolean ok = followService.unfollow(me.getId(), targetUserId);
        return ResponseEntity.ok(ok ? "取关成功" : "未关注该用户");
    }

    //查看我的关注列表
    @GetMapping("/follow/list/{userId}")
    public List<User> getMyFollowList(Principal principal) {
        User me = userRepository.findByEmail(principal.getName());

        List<Follows> list = followRepository.findByFollowerId(me.getId());

        return list.stream().map(f -> userRepository.findById(f.getFollowerId()).get()).toList();
    }

    @GetMapping("/follow/articles/{userId}")
    public List<Article> getFollowFeed(Principal principal) {
        User me = userRepository.findByEmail(principal.getName());

        //找到我关注的人
        List<Follows> follows = followRepository.findByFollowerId(me.getId());

        List<Long> followeeIds = follows.stream().map(Follows::getFolloweeId).toList();

        //查询这些人的公开文章或者仅粉丝可见的文章
        List<Article> feed = articleRepository.findByAuthorIdInOrderByCreateTimeDesc(followeeIds);

        return feed;
    }
}
