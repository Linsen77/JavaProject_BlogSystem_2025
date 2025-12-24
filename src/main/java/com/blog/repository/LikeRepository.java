package com.blog.repository;

import com.blog.entity.Article;
import com.blog.entity.Likes;
import com.blog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LikeRepository extends JpaRepository<Likes, Long> {

    //判断用户是否已点赞这篇文章
    boolean existsByUserAndArticle(User user, Article article);

    //统计用户发布的文章总点赞数
    @Query("SELECT COUNT(l) FROM Likes l WHERE l.article.author.id = :authorId")
    long countByArticleAuthorId(Long authorId);

    //用户是否给这篇文章点赞
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    //统计文章获赞数
    int countByArticleId(Long articleId);

    Likes findByArticleIdAndUserId(Long articleId, Long userId);

}
