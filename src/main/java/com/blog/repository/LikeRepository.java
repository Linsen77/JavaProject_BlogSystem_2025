package com.blog.repository;

import com.blog.entity.Article;
import com.blog.entity.Likes;
import com.blog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Likes, Long> {

    //判断用户是否已点赞这篇文章
    boolean existsByUserAndArticle(User user, Article article);


}
