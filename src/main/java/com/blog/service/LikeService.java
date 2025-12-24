package com.blog.service;


import com.blog.entity.Article;
import com.blog.entity.Likes;
import com.blog.entity.User;
import com.blog.repository.ArticleRepository;
import com.blog.repository.LikeRepository;
import com.blog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LikeService {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private UserRepository userRepository;

    //点赞功能
    public Likes addLike(Long articleId, Long userId){
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new RuntimeException("文章不存在"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));

        //检查用户是否已经点赞过这篇文章
        boolean alreadyLiked = likeRepository.existsByUserAndArticle(user,article);
        if(alreadyLiked){
            throw new RuntimeException("已经点赞过该文章");
        }

        Likes like = new Likes();
        like.setUser(user);
        like.setArticle(article);
        like.setCreateTime(LocalDateTime.now());

        //保存点赞
        Likes savedLike = likeRepository.save(like);

        //发送通知
        notificationService.sendNotificationToAuthor(article.getAuthor(), "你的文章收到了点赞~");

        return savedLike;

    }

    public void removeLike(Long articleId, Long userId) {
        Likes like = likeRepository.findByArticleIdAndUserId(articleId, userId);
        if (like != null) {
            likeRepository.delete(like);
        }
    }


    public boolean hasUserLikedArticle(Long userId, Long articleId) {
        return likeRepository.existsByUserIdAndArticleId(userId, articleId);
    }

    public int countLikes(Long articleId) {
        return likeRepository.countByArticleId(articleId);
    }

}
