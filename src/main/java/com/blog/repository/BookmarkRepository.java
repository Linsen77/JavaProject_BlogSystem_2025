package com.blog.repository;

import com.blog.entity.Article;
import com.blog.entity.Bookmark;
import com.blog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    //检查是否已收藏文章
    boolean existsByUserAndArticle(User user, Article article);

    //通过userId来查找收藏的文章
    List<Article> findByUserId(Long userId);
}
